import { Table } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import SeverityBadge from './SeverityBadge';
import type { AlertSummary } from '../types';

export default function AlertsTable({ alerts }: { alerts: AlertSummary[] }) {
    if (alerts.length === 0) {
        return <p className="text-muted my-4">No alerts match these filters.</p>;
    }
    return (
        <Table striped hover responsive size="sm">
            <thead>
            <tr>
                <th>Created</th><th>Rule</th><th>Trader</th><th>Symbol</th><th>Severity</th><th>Status</th>
            </tr>
            </thead>
            <tbody>
            {alerts.map((a) => (
                <tr key={a.id}>
                    <td>{new Date(a.createdAt).toLocaleString()}</td>
                    <td><Link to={`/alerts/${a.id}`}>{a.rule}</Link></td>
                    <td>{a.traderId}</td>
                    <td>{a.symbol ?? '-'}</td>
                    <td><SeverityBadge severity={a.severity} /></td>
                    <td>{a.status}</td>
                </tr>
            ))}
            </tbody>
        </Table>
    );
}