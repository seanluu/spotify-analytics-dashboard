'use client';

import { ArrowTopRightOnSquareIcon } from '@heroicons/react/24/solid';
import { Artist } from '@/types';
import { useSimpleApi } from '@/hooks/useSimpleApi';

interface TopArtistsProps {
  timeRange: string;
}

export function TopArtists({ timeRange }: TopArtistsProps) {
  const { data: artists, isLoading } = useSimpleApi<Artist>('/spotify/top/artists', { time_range: timeRange, limit: 50 });

  if (isLoading) {
    return (
      <div className="card">
        <div className="flex justify-center py-8">
          <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    );
  }

  return (
    <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-heading">🎤 Top Artists</h2>
        </div>
      
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {artists.map((artist, index) => (
          <div
            key={artist.id}
            className="group cursor-pointer"
            onClick={() => window.open(artist.external_urls.spotify, '_blank')}
          >
            <div className="relative overflow-hidden rounded-lg bg-spotify-dark hover:bg-gray-800 transition-colors">
              <img
                src={artist.images[1]?.url || artist.images[0]?.url}
                alt={artist.name}
                className="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-300"
              />
              
              <div className="absolute top-2 left-2">
                <div className="w-8 h-8 bg-spotify-black bg-opacity-80 rounded-full flex items-center justify-center">
                  <span className="text-white font-bold text-sm">{index + 1}</span>
                </div>
              </div>
              
              <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <div className="w-8 h-8 bg-spotify-green rounded-full flex items-center justify-center">
                  <ArrowTopRightOnSquareIcon className="w-4 h-4 text-white" />
                </div>
              </div>
              
              <div className="p-4">
                <h3 className="text-white font-semibold text-lg mb-1 truncate">
                  {artist.name}
                </h3>
                
                
                <p className="text-sm text-gray-400 mb-3">
                  {artist.followers.total.toLocaleString()} followers
                </p>
                
                <div className="flex flex-wrap gap-1">
                  {artist.genres.slice(0, 2).map((genre, genreIndex) => (
                    <span
                      key={genreIndex}
                      className="px-2 py-1 bg-spotify-gray text-xs text-gray-300 rounded-full"
                    >
                      {genre}
                    </span>
                  ))}
                  {artist.genres.length > 2 && (
                    <span className="px-2 py-1 bg-spotify-gray text-xs text-gray-300 rounded-full">
                      +{artist.genres.length - 2}
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {artists.length === 0 && (
        <div className="text-center py-8">
          <p className="text-gray-400">No artists found for this time period</p>
        </div>
      )}
    </div>
  );
}

