import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CaseBoard from './CaseBoard';
import { AuthContext, type AuthState } from '../auth/AuthContext';
import type { CaseStatus, CaseSummary } from '../types';

// Replace the real Keycloak module: api() then gets a fixed token and never redirects.
vi.mock('../auth/keycloak', () => ({
    keycloak: { token: 'test-token', updateToken: () => Promise.resolve(true), login: vi.fn() },
}));

function caseIn(status: CaseStatus): CaseSummary {
    return {
        id: 'c-1', title: 'Spoofing T-77', status, assignee: null, priority: 3, openedBy: 'analyst1',
        openedAt: '2026-10-27T14:01:00Z', closedAt: null, version: 1, alertCount: 1,
    };
}

function renderBoard(cases: CaseSummary[], isSupervisor: boolean) {
    const auth: AuthState = { username: isSupervisor ? 'supervisor1' : 'analyst1', isSupervisor, logout: () => {} };
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AuthContext.Provider value={auth}>
                <MemoryRouter>
                    <CaseBoard cases={cases} />
                </MemoryRouter>
            </AuthContext.Provider>
        </QueryClientProvider>,
    );
}

afterEach(() => vi.restoreAllMocks());

describe('CaseBoard', () => {
    it('does not offer Close to an analyst', () => {
        renderBoard([caseIn('IN_REVIEW')], false);
        expect(screen.getByRole('button', { name: 'Move to Escalated' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
    });

    it('offers Close to a supervisor', () => {
        renderBoard([caseIn('IN_REVIEW')], true);
        expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
    });

    it('explains a 409 version conflict in plain words', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            new Response(JSON.stringify({ status: 409, title: 'Version conflict', detail: 'changed by someone else' }),
                { status: 409, headers: { 'Content-Type': 'application/problem+json' } }),
        );
        renderBoard([caseIn('OPEN')], false);

        await userEvent.click(screen.getByRole('button', { name: 'Move to In review' }));

        expect(await screen.findByText(/Someone else changed this case/)).toBeInTheDocument();
    });
});