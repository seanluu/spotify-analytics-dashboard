import { useEffect, useState } from "react";
import { api } from "@/lib/api";

// Helper function to log API errors
const logApiError = (error: any) => {
    console.error('Failed to fetch data:', error);
    if (error.response) {
        console.error('Response error:', error.response.status, error.response.data);
    } else if (error.request) {
        console.error('Request error:', error.request);
    }
};

export function useSimpleApi<T>(endpoint: string, params: Record<string, string | number> = {}) {
    const [data, setData] = useState<T[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const response = await api.get(endpoint, { params });
                const newData = response.data.items || response.data || [];
                setData(newData);
            } catch (error: any) {
                logApiError(error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();

    }, [endpoint, JSON.stringify(params)]);

    return { data, isLoading };
}

export function useSimpleApiSingle<T>(endpoint: string, params: Record<string, string | number> = {}, refreshKey: number = 0) {
    const [data, setData] = useState<T | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const response = await api.get(endpoint, { params });
                setData(response.data);
            } catch (error: any) {
                logApiError(error);
                setData(null);
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();
    }, [endpoint, JSON.stringify(params), refreshKey]);

    return { data, isLoading };
}