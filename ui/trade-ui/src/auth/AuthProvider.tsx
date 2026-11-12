import { useEffect, useState, type ReactNode } from 'react';
import { Alert, Spinner } from 'react-bootstrap';
import { keycloak, startAuth } from './keycloak';
import { AuthContext, type AuthState } from './AuthContext';

export function AuthProvider({ children }: { children: ReactNode }) {
    const [ready, setReady] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        startAuth()
            .then((authenticated) => setReady(authenticated))
            .catch(() => setError('Could not reach the login server. Is Keycloak running on port 8180?'));
    }, []);

    useEffect(() => {
        if (!ready) return;
        const id = setInterval(() => {
            keycloak.updateToken(30).catch(() => keycloak.login());   // refresh failed: log in again
        }, 20_000);
        return () => clearInterval(id);
    }, [ready]);

    if (error) return <Alert variant="danger" className="m-4">{error}</Alert>;
    if (!ready) {
        return (
            <div className="d-flex justify-content-center mt-5">
                <Spinner animation="border" role="status"><span className="visually-hidden">Signing in</span></Spinner>
            </div>
        );
    }

    const value: AuthState = {
        username: (keycloak.tokenParsed?.preferred_username as string | undefined) ?? 'unknown',
        isSupervisor: keycloak.hasRealmRole('SUPERVISOR'),
        logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    };
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}