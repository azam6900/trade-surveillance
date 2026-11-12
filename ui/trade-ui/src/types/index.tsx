export type AlertStatus = 'NEW' | 'IN_CASE' | 'DISMISSED';
export type CaseStatus = 'OPEN' | 'IN_REVIEW' | 'ESCALATED' | 'CLOSED';

export interface Page<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface AlertSummary {
    id: string;
    rule: string;
    traderId: string;
    symbol: string | null;
    severity: number;
    status: AlertStatus;
    windowStart: string;
    windowEnd: string;
    createdAt: string;
}

export interface AlertDetail {
    alert: AlertSummary;
    evidence: Record<string, unknown>;
}

/** What the live stream sends (AlertCreated on the server). */
export interface AlertCreated {
    alertId: string;
    rule: string;
    traderId: string;
    symbol: string | null;
    severity: number;
    createdAt: string;
}

export interface CaseSummary {
    id: string;
    title: string;
    status: CaseStatus;
    assignee: string | null;
    priority: number;
    openedBy: string;
    openedAt: string;
    closedAt: string | null;
    version: number;
    alertCount: number;
}

export interface CaseEventView {
    id: number;
    actor: string;
    action: string;
    detail: Record<string, unknown>;
    at: string;
}

export interface CaseDetail {
    summary: CaseSummary;
    alerts: AlertSummary[];
    events: CaseEventView[];
}