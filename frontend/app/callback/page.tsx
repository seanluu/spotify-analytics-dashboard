'use client';

import { Suspense, useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'next/navigation';
import Cookies from 'js-cookie';
import { exchangeCodeForTokens } from '@/lib/spotifyAuth';

function CallbackContent() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState('');
  const hasProcessed = useRef(false);

  useEffect(() => {
    // Prevent multiple executions
    if (hasProcessed.current) return;
    
    const code = searchParams.get('code');
    const error = searchParams.get('error');

    // Clean up URL
    window.history.replaceState({}, document.title, '/callback');

    if (error) {
      hasProcessed.current = true;
      setStatus('error');
        setErrorMessage('Access denied. Please try again.');
      return;
    }

    if (!code) {
      // Don't show error immediately - wait a bit in case code is still loading
      const timeout = setTimeout(() => {
        if (!hasProcessed.current) {
          hasProcessed.current = true;
          setStatus('error');
          setErrorMessage('No authorization code received. Please try again.');
        }
      }, 500);
      return () => clearTimeout(timeout);
    }

    // Mark as processing
    hasProcessed.current = true;

    // Exchange code for tokens via backend API
    let redirectTimer: ReturnType<typeof setTimeout> | null = null;
    
    exchangeCodeForTokens(code)
      .then((tokenData) => {
        // Store access token in cookie
        const accessToken = tokenData.access_token;
        if (!accessToken) {
          setStatus('error');
          setErrorMessage('No access token received. Please try again.');
          return;
        }

        // expires_in is in seconds, convert to days for cookie expiration
        // Spotify tokens typically expire in 3600 seconds (1 hour)
        const expiresInDays = tokenData.expires_in ? Math.max(1, Math.ceil(tokenData.expires_in / 86400)) : 1;
        
        // Set cookie with explicit options
        Cookies.set('spotify_access_token', accessToken, { 
          expires: expiresInDays,
          secure: window.location.protocol === 'https:',
          sameSite: 'lax',
          path: '/'
        });
        
        // Verify cookie was set
        const cookieValue = Cookies.get('spotify_access_token');
        if (!cookieValue) {
          console.error('Failed to set cookie');
          setStatus('error');
          setErrorMessage('Failed to save authentication token. Please try again.');
          return;
        }
        
        setStatus('success');
        
        // Redirect after a short delay to ensure cookie is saved
        redirectTimer = setTimeout(() => {
          // Force a hard redirect to ensure cookie is read
          window.location.href = '/';
        }, 1500);
      })
      .catch((err) => {
        console.error('Token exchange error:', err);
        setStatus('error');
        setErrorMessage(err.message || 'Failed to exchange authorization code. Please try again.');
      });
    
    // Cleanup function
    return () => {
      if (redirectTimer) {
        clearTimeout(redirectTimer);
      }
    };
  }, [searchParams]);

  if (status === 'loading') {
    return (
      <div className="min-h-screen bg-spotify-black flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="min-h-screen bg-spotify-black flex items-center justify-center p-4">
        <div className="text-center">
          <div className="w-16 h-16 bg-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="text-white text-2xl">✕</span>
          </div>
          <h1 className="text-2xl font-bold text-white mb-4">Authentication Failed</h1>
          <p className="text-gray-400 mb-6">{errorMessage}</p>
          <button
            onClick={() => {
              window.location.href = '/';
            }}
            className="btn-primary"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-spotify-black flex items-center justify-center p-4">
      <div className="text-center">
        <div className="w-16 h-16 bg-spotify-green rounded-full flex items-center justify-center mx-auto mb-4">
          <span className="text-white text-2xl">✓</span>
        </div>
        <h1 className="text-2xl font-bold text-white mb-4">Successfully Connected!</h1>
        <p className="text-gray-400 mb-6">Redirecting to your dashboard...</p>
        <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <button
          onClick={() => {
            window.location.href = '/';
          }}
          className="btn-primary mt-4"
        >
          Go to Dashboard
        </button>
      </div>
    </div>
  );
}

export default function CallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-spotify-black flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin" />
      </div>
    }>
      <CallbackContent />
    </Suspense>
  );
}
