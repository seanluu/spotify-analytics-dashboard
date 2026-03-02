'use client';

import { useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { useSimpleApiSingle } from '@/hooks/useSimpleApi';
import { api } from '@/lib/api';
import { ArrowPathIcon } from '@heroicons/react/24/solid';
import toast from 'react-hot-toast';

interface AudioInsightsProps {
  timeRange: string;
}

interface AudioInsightsData {
  totalTracks: number;
  tracksWithFeatures: number;
  averages: {
    energy: number;
    valence: number;
    danceability: number;
    tempo: number;
    acousticness: number;
  };
  mood: string;
  message?: string;
}

export function AudioInsights({ timeRange }: AudioInsightsProps) {
  const [refreshKey, setRefreshKey] = useState(0);
  const [isFetching, setIsFetching] = useState(false);
  const { data: insights, isLoading } = useSimpleApiSingle<AudioInsightsData>(
    '/spotify/audio-features/insights-from-top',
    { time_range: timeRange },
    refreshKey
  );

  const fetchAudioFeatures = async () => {
    try {
      setIsFetching(true);
      await api.post('/spotify/audio-features/fetch', null, {
        params: {
          timeRange,
          limit: 50
        }
      });
      toast.success('Audio features fetched successfully! Refreshing...');
      // trigger refresh by updating the key
      setRefreshKey(prev => prev + 1);
    } catch (error: any) {
      console.error('Failed to fetch audio features:', error);
      toast.error('Failed to fetch audio features. Please try again.');
    } finally {
      setIsFetching(false);
    }
    };

  if (isLoading) {
    return (
      <div className="card">
        <div className="flex justify-center py-8">
      <div className="w-8 h-8 border-2 border-spotify-green border-t-transparent rounded-full animate-spin" />
    </div>
    </div>
  );
  }

  const RefreshButton = () => (
    <button
      onClick={fetchAudioFeatures}
      disabled={isFetching}
      className="btn-primary flex items-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
    >
      {isFetching ? (
        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
      ) : (
        <ArrowPathIcon className="w-4 h-4" />
      )}
      <span>{isFetching ? 'Fetching...' : 'Refresh Features'}</span>
    </button>
  );

  if (!insights || insights.message) {
    return (
      <div className="card">
        <div className="mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-heading">🎧 Audio Insights</h2>
            </div>
            <RefreshButton />
          </div>
        </div>
        <div className="text-center py-8">
          <p className="text-gray-400">{insights?.message || 'No audio insights available'}</p>
        </div>
      </div>
    );
  }

  const chartData = [
    { name: 'Energy', value: insights.averages.energy },
    { name: 'Valence', value: insights.averages.valence },
    { name: 'Danceability', value: insights.averages.danceability },
    { name: 'Acousticness', value: insights.averages.acousticness },
  ];

  const getMoodEmoji = (mood: string) => {
    const moodMap: Record<string, string> = {
      'Happy': '😊',
      'Energetic': '⚡',
      'Calm': '😌',
      'Sad': '😢',
      'Neutral': '😐',
    };
    return moodMap[mood] || '🎵';
  };

  return (
      <div className="card">
      <div className="mb-6">
        <div className="flex items-center justify-between">
          <div>
        <h2 className="text-heading">Audio Insights</h2>
        <p className="text-gray-400 text-sm mt-2">
          Analysis of {insights.tracksWithFeatures} out of {insights.totalTracks} tracks
        </p>
          </div>
          <RefreshButton />
        </div>
        </div>

      <div className="space-y-6">
        {/* Mood & Tempo */}
        <div className="bg-spotify-dark rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-400 text-sm">Overall Mood</p>
              <p className="text-white text-2xl font-bold mt-1">
                {getMoodEmoji(insights.mood)} {insights.mood}
            </p>
          </div>
            <div className="text-right">
              <p className="text-gray-400 text-sm">Avg Tempo</p>
              <p className="text-white text-2xl font-bold mt-1">
                {insights.averages.tempo.toFixed(0)} BPM
              </p>
        </div>
        </div>
      </div>

        {/* Chart */}
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
              <XAxis dataKey="name" stroke="#9CA3AF" />
              <YAxis domain={[0, 1]} stroke="#9CA3AF" />
              <Tooltip 
                contentStyle={{
                  backgroundColor: '#1F2937',
                  border: '1px solid #374151',
                  borderRadius: '8px',
                }}
                labelStyle={{ color: '#F3F4F6' }}
              />
              <Legend />
              <Bar dataKey="value" fill="#1DB954" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-spotify-dark rounded-lg p-4">
            <p className="text-gray-400 text-sm">Energy</p>
            <p className="text-white text-xl font-bold mt-1">
              {(insights.averages.energy * 100).toFixed(0)}%
            </p>
          </div>
          <div className="bg-spotify-dark rounded-lg p-4">
            <p className="text-gray-400 text-sm">Valence</p>
            <p className="text-white text-xl font-bold mt-1">
              {(insights.averages.valence * 100).toFixed(0)}%
            </p>
          </div>
          <div className="bg-spotify-dark rounded-lg p-4">
            <p className="text-gray-400 text-sm">Danceability</p>
            <p className="text-white text-xl font-bold mt-1">
              {(insights.averages.danceability * 100).toFixed(0)}%
            </p>
          </div>
          <div className="bg-spotify-dark rounded-lg p-4">
            <p className="text-gray-400 text-sm">Acousticness</p>
            <p className="text-white text-xl font-bold mt-1">
              {(insights.averages.acousticness * 100).toFixed(0)}%
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

