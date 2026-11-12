import type { ReactNode } from 'react';
import { Alert } from 'react-bootstrap';
import { useAuth } from '../auth/AuthContext';

export default function RequireRole({ role, children }: { role: 'SUPERVISOR'; children: ReactNode }) {
    const { isSupervisor } = useAuth();
    if (role === 'SUPERVISOR' && !isSupervisor) {
        return <Alert variant="warning">Not allowed: this page is for supervisors only.</Alert>;
    }
    return <>{children}</>;
}