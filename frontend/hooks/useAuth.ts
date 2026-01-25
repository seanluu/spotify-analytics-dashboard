'use client';

import { useEffect, useState } from "react";
import Cookies from "js-cookie";
import { User } from "@/types";
import { api } from "@/lib/api";
import { getAuthUrl } from "@/lib/spotifyAuth";

export function useAuth() {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        checkAuthStatus();
    }, []);

    const checkAuthStatus = async () => {
        const token = Cookies.get('spotify_access_token');
        if (!token) {
            setIsLoading(false);
            return;
        }

        try {
            const response = await api.get('/spotify/me');
            if (response.data) {
                setUser(response.data);
            }
        } catch (error: unknown) {
            console.error('Auth check failed:', error);
            Cookies.remove('spotify_access_token');
        } finally {
            setIsLoading(false);
        }
    };

    const login = () => {
        window.location.href = getAuthUrl() + '&show_dialog=true';
    };

    const logout = () => {
        Cookies.remove('spotify_access_token');
        setUser(null);
        window.location.href = '/';
    };

    return {
        user,
        isLoading,
        login,
        logout
    };
}