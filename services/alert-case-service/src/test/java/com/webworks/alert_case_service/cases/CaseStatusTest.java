package com.webworks.alert_case_service.cases;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CaseStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @CsvSource({
            "OPEN, OPEN, false",           "OPEN, IN_REVIEW, true",        "OPEN, ESCALATED, false",       "OPEN, CLOSED, false",
            "IN_REVIEW, OPEN, false",      "IN_REVIEW, IN_REVIEW, false",  "IN_REVIEW, ESCALATED, true",   "IN_REVIEW, CLOSED, true",
            "ESCALATED, OPEN, false",      "ESCALATED, IN_REVIEW, false",  "ESCALATED, ESCALATED, false",  "ESCALATED, CLOSED, true",
            "CLOSED, OPEN, false",         "CLOSED, IN_REVIEW, false",     "CLOSED, ESCALATED, false",     "CLOSED, CLOSED, false"
    })
    void onlyTheDrawnMovesAreAllowed(CaseStatus from, CaseStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }
}