import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap';
import { api } from '../api/client';
import EvidenceChart from '../components/EvidenceChart.tsx';
import SeverityBadge from '../components/SeverityBadge';
import type { AlertDetail, CaseDetail, EvidenceEvent } from '../types';
import { usePageTitle } from '../hooks/usePageTitle';

const time = (iso: string) => new Date(iso).toLocaleTimeString();

export default function AlertDetailPage() {
    const { id } = useParams<{ id: string }>();
    usePageTitle('Alert ' + id);
    const navigate = useNavigate();
    const [showDialog, setShowDialog] = useState(false);
    const [title, setTitle] = useState('');

    const { data, isPending, error } = useQuery({
        queryKey: ['alert', id],
        queryFn: () => api<AlertDetail>(`/api/alerts/${id}`),
    });

    const createCase = useMutation({
        mutationFn: () => api<CaseDetail>('/api/cases', { method: 'POST', body: JSON.stringify({ title, alertIds: [id] }) }),
        onSuccess: (created) => navigate(`/cases/${created.summary.id}`),
    });

    if (isPending) return <Spinner animation="border" role="status"><span className="visually-hidden">Loading alert</span></Spinner>;
    if (error) return <Alert variant="danger">Could not load this alert: {error.message}</Alert>;

    const { alert, evidence } = data;
    const events = (evidence.events as EvidenceEvent[] | undefined) ?? [];

    return (
        <>
            <Card className="mb-3">
                <Card.Body>
                    <Row className="align-items-center">
                        <Col md={8}>
                            <h1 className="h4 mb-1">{alert.rule} · {alert.traderId}{alert.symbol ? ` · ${alert.symbol}` : ''}</h1>
                            <div className="text-muted small">
                                Window {time(alert.windowStart)} to {time(alert.windowEnd)} · status {alert.status}
                            </div>
                        </Col>
                        <Col md={4} className="text-md-end mt-2 mt-md-0">
                            <SeverityBadge severity={alert.severity} />
                            <Button className="ms-2" disabled={alert.status !== 'NEW'}
                                    onClick={() => { setTitle(`${alert.rule} ${alert.traderId}`); setShowDialog(true); }}>
                                Create case
                            </Button>
                        </Col>
                    </Row>
                </Card.Body>
            </Card>

            {events.length > 0 ? (
                <>
                    <Card className="mb-3">
                        <Card.Body>
                            <Card.Title as="h2" className="h6">Order activity in the window</Card.Title>
                            <EvidenceChart events={events} />
                        </Card.Body>
                    </Card>
                    <Table size="sm" striped responsive>
                        <thead><tr><th>Time</th><th>Action</th><th>Side</th><th>Quantity</th><th>Price</th><th>Order</th></tr></thead>
                        <tbody>
                        {events.map((e, i) => (
                            <tr key={`${e.orderId}-${i}`}>
                                <td>{time(e.ts)}</td><td>{e.action}</td><td>{e.side}</td>
                                <td>{e.qty.toLocaleString()}</td><td>{e.price}</td><td><code>{e.orderId}</code></td>
                            </tr>
                        ))}
                        </tbody>
                    </Table>
                </>
            ) : (
                <Table size="sm" className="w-auto">
                    <tbody>
                    {Object.entries(evidence).map(([key, value]) => (
                        <tr key={key}><th scope="row">{key}</th><td>{String(value)}</td></tr>
                    ))}
                    </tbody>
                </Table>
            )}

            <Modal show={showDialog} onHide={() => setShowDialog(false)}>
                <Form onSubmit={(e) => { e.preventDefault(); createCase.mutate(); }}>
                    <Modal.Header closeButton><Modal.Title>Create a case</Modal.Title></Modal.Header>
                    <Modal.Body>
                        <Form.Group controlId="case-title">
                            <Form.Label>Title</Form.Label>
                            <Form.Control value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} autoFocus />
                        </Form.Group>
                        {createCase.isError && <Alert variant="danger" className="mt-3">{createCase.error.message}</Alert>}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={() => setShowDialog(false)}>Cancel</Button>
                        <Button type="submit" disabled={createCase.isPending || !title.trim()}>Create</Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </>
    );
}