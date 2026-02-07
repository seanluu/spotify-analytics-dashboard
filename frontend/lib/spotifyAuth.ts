// Helper function to get the redirect URI
// Use 127.0.0.1 instead of localhost for better compatibility with Spotify
const getRedirectUri = (): string => {
  return process.env.NEXT_PUBLIC_SPOTIFY_REDIRECT_URI || 
    (typeof window !== 'undefined' 
      ? `${window.location.origin}/callback`
      : 'http://127.0.0.1:3000/callback');
};

const normalizeBaseUrl = (raw: string): string => {
  const trimmed = raw.trim().replace(/\/+$/, '');
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed;
  if (trimmed.startsWith('/')) return trimmed;
  return `/${trimmed}`;
};

export const getAuthUrl = () => {
  const clientId = process.env.NEXT_PUBLIC_SPOTIFY_CLIENT_ID;
  if (!clientId) {
    console.error('NEXT_PUBLIC_SPOTIFY_CLIENT_ID is not set. Check your .env.local file.');
    throw new Error('NEXT_PUBLIC_SPOTIFY_CLIENT_ID is not set');
  }
  
  const params = new URLSearchParams({
    client_id: clientId,
    response_type: 'code',
    redirect_uri: getRedirectUri(),
    scope: 'user-read-private user-read-email user-top-read playlist-read-private playlist-modify-public playlist-modify-private',
  });
  return `https://accounts.spotify.com/authorize?${params.toString()}`;
};

export const exchangeCodeForTokens = async (code: string) => {
  const API_BASE_URL = normalizeBaseUrl(process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080');
  
  // Send code and redirect_uri as form data (backend expects @RequestParam)
  const formData = new URLSearchParams();
  formData.append('code', code);
  formData.append('redirect_uri', getRedirectUri());
  
  const response = await fetch(`${API_BASE_URL}/api/v1/spotify/auth/callback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: formData.toString(),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
    // Handle both ErrorResponse format {error, message} and simple {error} format
    const errorMessage = errorData.message || errorData.error || `Failed to exchange code for tokens (Status: ${response.status})`;
    throw new Error(errorMessage);
  }

  const data = await response.json();
  return data;
};
