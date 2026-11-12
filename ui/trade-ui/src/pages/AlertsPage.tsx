import { useState, type ChangeEvent } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Alert, Col, Form, Pagination, Row, Spinner } from 'react-bootstrap';
import { api } from '../api/client';
import AlertsTable from '../components/AlertsTable';
import type { AlertSummary, Page } from '../types';

interface Filters { rule: string; status: string; traderId: string }

function toQuery(f: Filters, page: number): string {
    const params = new URLSearchParams({ page: String(page), size: '20', sort: 'createdAt,desc' });
    if (f.rule) params.set('rule', f.rule);
    if (f.status) params.set('status', f.status);
    if (f.traderId.trim()) params.set('traderId', f.traderId.trim());
    return params.toString();
}

export default function AlertsPage() {
    const [filters, setFilters] = useState<Filters>({ rule: '', status: '', traderId: '' });
    const [page, setPage] = useState(0);

    const { data, isPending, error } = useQuery({
        queryKey: ['alerts', filters, page],
        queryFn: () => api<Page<AlertSummary>>(`/api/alerts?${toQuery(filters, page)}`),
        placeholderData: keepPreviousData,     // keep the old rows on screen while the next page loads
    });

    const change = (field: keyof Filters) =>
        (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        setFilters({ ...filters, [field]: e.target.value });
        setPage(0);                            // new filter: back to the first page
    };

    return (
        <>
            <h1 className="h4 mb-3">Alerts</h1>
            <Form className="mb-3">
                <Row className="g-2">
                    <Col md={3}>
                        <Form.Group controlId="filter-rule">
                            <Form.Label>Rule</Form.Label>
                            <Form.Select value={filters.rule} onChange={change('rule')}>
                                <option value="">All rules</option>
                                <option value="SPOOFING">Spoofing</option>
                                <option value="CANCEL_RATIO">Cancel ratio</option>
                            </Form.Select>
                        </Form.Group>
                    </Col>
                    <Col md={3}>
                        <Form.Group controlId="filter-status">
                            <Form.Label>Status</Form.Label>
                            <Form.Select value={filters.status} onChange={change('status')}>
                                <option value="">All statuses</option>
                                <option value="NEW">New</option>
                                <option value="IN_CASE">In a case</option>
                                <option value="DISMISSED">Dismissed</option>
                            </Form.Select>
                        </Form.Group>
                    </Col>
                    <Col md={3}>
                        <Form.Group controlId="filter-trader">
                            <Form.Label>Trader</Form.Label>
                            <Form.Control placeholder="for example T-77" value={filters.traderId} onChange={change('traderId')} />
                        </Form.Group>
                    </Col>
                </Row>
            </Form>

            {isPending && <Spinner animation="border" role="status"><span className="visually-hidden">Loading alerts</span></Spinner>}
            {error && <Alert variant="danger">Could not load alerts: {error.message}</Alert>}
            {data && (
                <>
                    <AlertsTable alerts={data.content} />
                    {data.totalPages > 1 && (
                        <Pagination>
                            <Pagination.Prev disabled={page === 0} onClick={() => setPage(page - 1)} />
                            <Pagination.Item active>{`Page ${page + 1} of ${data.totalPages}`}</Pagination.Item>
                            <Pagination.Next disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} />
                        </Pagination>
                    )}
                </>
            )}
        </>
    );
}