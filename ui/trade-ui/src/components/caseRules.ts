import type { CaseStatus } from '../types';

export const STATUSES: CaseStatus[] = ['OPEN', 'IN_REVIEW', 'ESCALATED', 'CLOSED'];

export const LABEL: Record<CaseStatus, string> = {
    OPEN: 'Open', IN_REVIEW: 'In review', ESCALATED: 'Escalated', CLOSED: 'Closed',
};

/** Same moves as CaseStatus.canMoveTo on the server (Step 4.1). */
export const NEXT: Record<CaseStatus, CaseStatus[]> = {
    OPEN: ['IN_REVIEW'],
    IN_REVIEW: ['ESCALATED', 'CLOSED'],
    ESCALATED: ['CLOSED'],
    CLOSED: [],
};