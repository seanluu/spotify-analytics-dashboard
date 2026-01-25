'use client';

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { GenreData } from '@/types';
import { useSimpleApi } from '@/hooks/useSimpleApi';

// Color palette for genre visualization
const GENRE_COLORS = ['#1DB954', '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE'];

interface TopGenresProps {
  timeRange: string;
}

export function TopGenres({ timeRange }: TopGenresProps) {
  const { data: genres, isLoading } = useSimpleApi<GenreData>('/spotify/analytics/genres', { time_range: timeRange });

  if (isLoading) {
    return (
      <div className="card">
        <div className="flex items-center justify-center py-12">
          <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    );
  }

  interface TooltipPayload {
    payload: {
      name: string;
      count: number;
      percentage: string;
    };
  }

  const CustomTooltip = ({ active, payload }: { active?: boolean; payload?: TooltipPayload[] }) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="bg-spotify-dark border border-gray-700 rounded-lg p-3">
          <p className="text-white font-medium">{data.name}</p>
          <p className="text-spotify-green">
            {data.count} tracks ({data.percentage}%)
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="card">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-white">🎭 Top Genres</h2>
      </div>

      {genres.length > 0 ? (
        <div className="space-y-6">
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={genres}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percentage }) => `${name} (${percentage}%)`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="count"
                >
                  {genres.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={GENRE_COLORS[index % GENRE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">Genre Breakdown</h3>
            {genres.map((genre, index) => (
              <div key={genre.name} className="flex items-center justify-between p-3 rounded-lg bg-spotify-dark hover:bg-gray-800 transition-colors">
                <div className="flex items-center space-x-3">
                  <div
                    className="w-4 h-4 rounded-full"
                    style={{ backgroundColor: GENRE_COLORS[index % GENRE_COLORS.length] }}
                  />
                  <span className="text-white font-medium">{genre.name}</span>
                </div>
                <div className="flex items-center space-x-4">
                  <span className="text-gray-400">{genre.count} tracks</span>
                  <span className="text-spotify-green font-semibold">{genre.percentage}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="text-center py-8">
          <p className="text-gray-400">No genre data available for this time period</p>
        </div>
      )}
    </div>
  );
}
