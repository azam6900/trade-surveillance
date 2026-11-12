import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, Badge, Button, Card, Col, Row, Stack } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { LABEL, NEXT, STATUSES } from './caseRules';
import type { CaseDetail, CaseStatus, CaseSummary } from '../types';

export default function CaseBoard({ cases }: { cases: CaseSummary[] }) {
    const { isSupervisor } = useAuth();
    const queryClient = useQueryClient();
    const [message, setMessage] = useState<{ variant: string; text: string } | null>(null);

    const move = useMutation({
        mutationFn: ({ c, to }: { c: CaseSummary; to: CaseStatus }) =>
            api<CaseDetail>(`/api/cases/${c.id}`, { method: 'PATCH', body: JSON.stringify({ status: to, version: c.version }) }),
        onSuccess: () => {
            setMessage(null);
            queryClient.invalidateQueries({ queryKey: ['cases'] });
        },
        onError: (err) => {
            if (err instanceof ApiError && err.status === 409) {
                setMessage({ variant: 'warning', text: 'Someone else changed this case, so the board was refreshed. Please try again.' });
                queryClient.invalidateQueries({ queryKey: ['cases'] });
            } else {
                setMessage({ variant: 'danger', text: err.message });
            }
        },
    });

    return (
        <>
            {message && (
            <Alert variant={message.variant} dismissible onClose={() => setMessage(null)}>{message.text}</Alert>
)}
    <Row xs={1} md={4} className="g-3">
        {STATUSES.map((status) => {
                    const column = cases.filter((c) => c.status === status);
                    return (
                        <Col key={status}>
                        <h2 className="h6 text-uppercase text-muted">{LABEL[status]} <Badge bg="secondary">{column.length}</Badge></h2>
                    <Stack gap={2}>
                        {column.map((c) => (
                                <Card key={c.id}>
                                <Card.Body>
                                    <Card.Title as="h3" className="h6"><Link to={`/cases/${c.id}`}>{c.title}</Link></Card.Title>
                    <div className="small text-muted mb-2">
                        Assignee: {c.assignee ?? 'nobody'} · Priority {c.priority} · {c.alertCount} alert(s)
                    </div>
                    <Stack direction="horizontal" gap={1} className="flex-wrap">
                        {NEXT[c.status]
                                .filter((to) => to !== 'CLOSED' || isSupervisor)     // Close is supervisor-only
                                .map((to) => (
                                    <Button key={to} size="sm" variant={to === 'CLOSED' ? 'outline-danger' : 'outline-primary'}
                    disabled={move.isPending} onClick={() => move.mutate({ c, to })}>
                    {to === 'CLOSED' ? 'Close' : `Move to ${LABEL[to]}`}
                    </Button>
                ))}
                    </Stack>
                    </Card.Body>
                    </Card>
                ))}
                    {column.length === 0 && <p className="small text-muted">No cases.</p>}
                    </Stack>
                    </Col>
                    );
                    })}
                </Row>
                </>
);
}