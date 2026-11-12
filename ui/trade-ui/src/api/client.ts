import { keycloak } from '../auth/keycloak';

export const API_URL = import.meta.env.VITE_API_URL as string;

/** An HTTP error with its status code, so screens can react to 403 or 409. */
export class ApiError extends Error {
    readonly status: number;
    constructor(status: number, message: string) {
        super(message);
        this.status = status;
        this.name = 'ApiError';
    }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
    await keycloak.updateToken(10).catch(() => keycloak.login());   // refresh if it expires within 10 s
    const res = await fetch(`${API_URL}${path}`, {
        ...init,
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${keycloak.token}`,
            ...init.headers,
        },
    });
    if (!res.ok) {
        const problem = await res.json().catch(() => ({}));            // RFC 7807 body, if any
        throw new ApiError(res.status, problem.detail ?? problem.title ?? res.statusText);
    }
    return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}