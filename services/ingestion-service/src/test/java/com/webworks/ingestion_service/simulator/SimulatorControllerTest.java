package com.webworks.ingestion_service.simulator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulatorControllerUnitTest {

    @Mock
    Simulator simulator;

    @InjectMocks
    SimulatorController controller;

    @Test
    void cancelStormReportsHowManyEventsWereSent() {
        when(simulator.cancelStorm("T-99")).thenReturn(82);

        Map<String, Object> body = controller.cancelStorm("T-99");

        assertThat(body).containsEntry("scenario", "cancel-storm")
                .containsEntry("traderId", "T-99")
                .containsEntry("eventsSent", 82);
    }

    @Test
    void spoofingReportsWhenTheScenarioStarts() {
        when(simulator.spoofing("T-77", "RY.TO")).thenReturn(1234L);

        Map<String, Object> body = controller.spoofing("T-77", "RY.TO");

        assertThat(body).containsEntry("scenario", "spoofing")
                .containsEntry("symbol", "RY.TO")
                .containsEntry("startsInMs", 1234L);
    }

    @Test
    void rateWithinRangeIsApplied() {
        Map<String, Object> body = controller.rate(2000);

        verify(simulator).setEventsPerSecond(2000);
        assertThat(body).containsEntry("eventsPerSecond", 2000);
    }

    @Test
    void rateOutsideRangeIsRejected() {
        assertThatThrownBy(() -> controller.rate(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.rate(50_001)).isInstanceOf(IllegalArgumentException.class);
        verify(simulator, never()).setEventsPerSecond(anyInt());
    }
}