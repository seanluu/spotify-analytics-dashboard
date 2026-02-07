'use client';

import { useAuth } from '@/hooks/useAuth';
import { LoginPage } from '@/components/LoginPage';
import { Dashboard } from '@/components/Dashboard';

export default function HomePage() {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-spotify-black flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  return <Dashboard user={user} />;
}

