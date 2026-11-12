package com.webworks.alert_case_service.cases;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(CaseStatus from, CaseStatus to) {
        super("A case cannot move from " + from + " to " + to);
    }
}