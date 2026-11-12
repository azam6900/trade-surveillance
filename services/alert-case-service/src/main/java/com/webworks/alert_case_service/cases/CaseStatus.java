package com.webworks.alert_case_service.cases;

public enum CaseStatus {
    OPEN, IN_REVIEW, ESCALATED, CLOSED;

    public boolean canMoveTo(CaseStatus next) {
        return switch (this) {
            case OPEN -> next == IN_REVIEW;
            case IN_REVIEW -> next == ESCALATED || next == CLOSED;
            case ESCALATED -> next == CLOSED;
            case CLOSED -> false;
        };
    }
}