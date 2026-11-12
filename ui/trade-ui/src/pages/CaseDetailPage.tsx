import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Badge, Button, Card, Col, Form, ListGroup, Row, Spinner } from 'react-bootstrap';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { LABEL } from '../components/caseRules';
import SeverityBadge from '../components/SeverityBadge';
import type { CaseDetail, CaseEventView } from '../types';
import { usePageTitle } from '../hooks/usePageTitle';

function describe(e: CaseEventView): string {
    const d = e.detail as Record<string, string | undefined>;
    switch (e.action) {
        case 'CASE_OPENED': return 'opened the case';
        case 'STATUS_CHANGED': return `moved it from ${d.from} to ${d.to}`;
        case 'ASSIGNED': return `assigned it to ${d.to}`;
        case 'PRIORITY_CHANGED': return `changed priority from ${d.from} to ${d.to}`;
        case 'COMMENT_ADDED': return `commented: "${d.text}"`;
        default: return e.action;
    }
}

export default function CaseDetailPage() {
    const { id } = useParams<{ id: string }>();
    usePageTitle('Case ' + id);
    const { username } = useAuth();
    const queryClient = useQueryClient();
    const [text, setText] = useState('');

    const { data, isPending, error } = useQuery({
        queryKey: ['case', id],
        queryFn: () => api<CaseDetail>(`/api/cases/${id}`),
    });

    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['case', id] });
        queryClient.invalidateQueries({ queryKey: ['cases'] });
    };

    const comment = useMutation({
        mutationFn: () => api(`/api/cases/${id}/comments`, { method: 'POST', body: JSON.stringify({ text }) }),
        onSuccess: () => { setText(''); refresh(); },
    });

    const assignToMe = useMutation({
        mutationFn: (version: number) =>
            api(`/api/cases/${id}`, { method: 'PATCH', body: JSON.stringify({ assignee: username, version }) }),
        onSuccess: refresh,
    });

    if (isPending) return <Spinner animation="border" role="status"><span className="visually-hidden">Loading case</span></Spinner>;
    if (error) return <Alert variant="danger">Could not load this case: {error.message}</Alert>;

    const { summary, alerts, events } = data;
    const comments = events.filter((e) => e.action === 'COMMENT_ADDED');
    const submit = (e: FormEvent) => { e.preventDefault(); if (text.trim()) comment.mutate(); };

    return (
        <>
            <Card className="mb-3">
                <Card.Body>
                    <h1 className="h4">{summary.title} <Badge bg="info" text="dark">{LABEL[summary.status]}</Badge></h1>
                    <div className="text-muted small mb-2">
                        Opened by {summary.openedBy} on {new Date(summary.openedAt).toLocaleString()} · Priority {summary.priority} ·
                        Assignee {summary.assignee ?? 'nobody'}
                    </div>
                    {summary.assignee !== username && summary.status !== 'CLOSED' && (
                        <Button size="sm" variant="outline-secondary" onClick={() => assignToMe.mutate(summary.version)}>Assign to me</Button>
                    )}
                    {assignToMe.isError && <Alert variant="danger" className="mt-2">{assignToMe.error.message}</Alert>}
                </Card.Body>
            </Card>

            <Row className="g-3">
                <Col md={6}>
                    <h2 className="h6">Linked alerts</h2>
                    <ListGroup className="mb-4">
                        {alerts.map((a) => (
                            <ListGroup.Item key={a.id} action as={Link} to={`/alerts/${a.id}`}>
                                {a.rule} · {a.traderId} {a.symbol ?? ''} <SeverityBadge severity={a.severity} />
                            </ListGroup.Item>
                        ))}
                    </ListGroup>

                    <h2 className="h6">Comments</h2>
                    {comments.length === 0 && <p className="small text-muted">No comments yet.</p>}
                    <ListGroup className="mb-3">
                        {comments.map((c) => (
                            <ListGroup.Item key={c.id}><strong>{c.actor}</strong>: {String(c.detail.text)}</ListGroup.Item>
                        ))}
                    </ListGroup>
                    <Form onSubmit={submit}>
                        <Form.Group controlId="comment-text" className="mb-2">
                            <Form.Label>Add a comment</Form.Label>
                            <Form.Control as="textarea" rows={3} maxLength={2000} value={text} onChange={(e) => setText(e.target.value)} />
                        </Form.Group>
                        <Button type="submit" disabled={comment.isPending || !text.trim()}>Add comment</Button>
                        {comment.isError && <Alert variant="danger" className="mt-2">{comment.error.message}</Alert>}
                    </Form>
                </Col>

                <Col md={6}>
                    <h2 className="h6">Audit trail</h2>
                    <ListGroup as="ol" numbered>
                        {events.map((e) => (
                            <ListGroup.Item as="li" key={e.id}>
                                <strong>{e.actor}</strong> {describe(e)}
                                <div className="small text-muted">{new Date(e.at).toLocaleString()}</div>
                            </ListGroup.Item>
                        ))}
                    </ListGroup>
                </Col>
            </Row>
        </>
    );
}