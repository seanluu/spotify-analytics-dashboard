'use client';

import { useState } from 'react';
import { Track } from '@/types';
import { useSimpleApi } from '@/hooks/useSimpleApi';
import { api } from '@/lib/api';
import { PlusIcon } from '@heroicons/react/24/solid';
import toast from 'react-hot-toast';

interface TopTracksProps {
  timeRange: string;
}

export function TopTracks({ timeRange }: TopTracksProps) {
  const { data: tracks, isLoading } = useSimpleApi<Track>('/api/spotify/top/tracks', { time_range: timeRange, limit: 50 });
  const [isGenerating, setIsGenerating] = useState(false);

  const formatDuration = (ms: number): string => {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const renderTrack = (track: Track, index: number) => (
    <div key={track.id} className="flex items-center space-x-4 p-3 rounded-lg hover:bg-spotify-dark transition-colors group">
      <div className="w-8 text-center text-gray-400 font-medium">{index + 1}</div>
      
      <img
        src={track.album.images[2]?.url || track.album.images[0]?.url}
        alt={track.album.name}
        className="w-12 h-12 rounded object-cover"
      />
      
      <div className="flex-1 min-w-0">
        <a
          href={track.external_urls.spotify}
          target="_blank"
          rel="noopener noreferrer"
          className="block"
        >
          <h3 className="text-white font-medium truncate group-hover:text-spotify-green transition-colors cursor-pointer">
            {track.name}
          </h3>
        </a>
        <p className="text-gray-400 text-sm truncate">
          {track.artists.map(artist => artist.name).join(', ')}
        </p>
        <p className="text-gray-500 text-xs truncate">{track.album.name}</p>
      </div>
      
      <div className="flex items-center">
        <div className="text-sm text-gray-400">{formatDuration(track.duration_ms)}</div>
      </div>
    </div>
  );

  const generatePlaylist = async () => {
    try {
      setIsGenerating(true);
      
      const response = await api.post('/api/spotify/playlists/generate', {
        template: 'top-tracks',
        time_range: timeRange,
        name: `My Top Tracks - ${timeRange === 'short_term' ? 'Last 4 weeks' : timeRange === 'medium_term' ? 'Last 6 months' : 'All time'} - ${new Date().toLocaleDateString()}`,
        description: `Your top tracks from ${timeRange === 'short_term' ? 'last 4 weeks' : timeRange === 'medium_term' ? 'last 6 months' : 'all time'}`,
        public_playlist: false,
      });

      if (response.data) {
        toast.success(`"${response.data.name}" created successfully!`);
      }
    } catch (error) {
      console.error('Playlist creation error:', error);
      console.error('Error response:', error.response?.data);
      toast.error('Failed to create playlist. Please try again.');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Tracks List */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-heading">🎵 Top Tracks</h2>
          <button
            onClick={generatePlaylist}
            disabled={isGenerating || !tracks || tracks.length === 0}
            className="btn-primary flex items-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isGenerating ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <PlusIcon className="w-4 h-4" />
            )}
            <span>{isGenerating ? 'Creating...' : 'Create Playlist'}</span>
          </button>
        </div>
        
        {isLoading ? (
          <div className="flex justify-center py-8">
            <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin" />
          </div>
        ) : tracks.length === 0 ? (
          <div className="text-center py-8">
            <p className="text-gray-400">No tracks found for this time period</p>
          </div>
        ) : (
          <div className="space-y-3">
            {tracks.map((track, index) => renderTrack(track, index))}
          </div>
        )}
      </div>
    </div>
  );
}