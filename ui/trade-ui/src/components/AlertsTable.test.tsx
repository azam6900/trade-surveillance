import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AlertsTable from './AlertsTable';
import type { AlertSummary } from '../types';

function alert(id: string, severity: number): AlertSummary {
    return {
        id, rule: 'SPOOFING', traderId: 'T-77', symbol: 'RY.TO', severity, status: 'NEW',
        windowStart: '2026-10-27T14:00:00Z', windowEnd: '2026-10-27T14:00:10Z', createdAt: '2026-10-27T14:00:12Z',
    };
}

function renderTable(alerts: AlertSummary[]) {
    render(<MemoryRouter><AlertsTable alerts={alerts} /></MemoryRouter>);   // Link needs a router
}

describe('AlertsTable', () => {
    it('colours severity badges by level and labels them in words', () => {
        renderTable([alert('a', 80), alert('b', 60), alert('c', 20)]);
        expect(screen.getByText('High 80')).toHaveClass('bg-danger');
        expect(screen.getByText('Medium 60')).toHaveClass('bg-warning');
        expect(screen.getByText('Low 20')).toHaveClass('bg-secondary');
    });

    it('links each rule to its alert page', () => {
        renderTable([alert('abc', 80)]);
        expect(screen.getByRole('link', { name: 'SPOOFING' })).toHaveAttribute('href', '/alerts/abc');
    });

    it('shows a friendly message when there are no alerts', () => {
        renderTable([]);
        expect(screen.getByText('No alerts match these filters.')).toBeInTheDocument();
    });
});