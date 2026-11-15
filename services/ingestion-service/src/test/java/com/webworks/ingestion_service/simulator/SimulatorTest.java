package com.webworks.ingestion_service.simulator;

import com.webworks.ingestion_service.service.OrderPublisher;
import com.webworks.ingestion_service.service.OrderValidator;
import com.webworks.tradecommon.OrderEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Same package as Simulator on purpose: tick() and shutdown() are package-private.
@ExtendWith(MockitoExtension.class)
class SimulatorTest {

    @Mock
    OrderPublisher publisher;

    private Simulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new Simulator(publisher, 50);
    }

    @AfterEach
    void tearDown() {
        simulator.shutdown();   // cancels anything the spoofing scenario scheduled
    }

    @Test
    void rateCanBeChanged() {
        assertThat(simulator.getEventsPerSecond()).isEqualTo(50);
        simulator.setEventsPerSecond(7);
        assertThat(simulator.getEventsPerSecond()).isEqualTo(7);
    }

    @Test
    void tickPublishesOneEventPerConfiguredRate() {
        simulator.tick();
        verify(publisher, times(50)).publish(any(OrderEvent.class));
    }

    @Test
    void tickAtZeroRatePublishesNothing() {
        simulator.setEventsPerSecond(0);
        simulator.tick();
        verify(publisher, never()).publish(any(OrderEvent.class));
    }

    @Test
    void backgroundTrafficMixesNewCancelAndFillAndIsAlwaysValid() {
        simulator.setEventsPerSecond(1000);
        simulator.tick();

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(publisher, times(1000)).publish(captor.capture());
        List<OrderEvent> events = captor.getAllValues();

        Set<OrderEvent.Action> actions = events.stream().map(OrderEvent::action).collect(Collectors.toSet());
        assertThat(actions).containsExactlyInAnyOrder(
                OrderEvent.Action.NEW, OrderEvent.Action.CANCEL, OrderEvent.Action.FILL);
        assertThat(events).allSatisfy(e -> {
            assertThat(OrderValidator.isValid(e)).isTrue();
            assertThat(e.traderId()).matches("T-\\d{2}");
            assertThat(e.orderId()).startsWith("O-");
        });
    }

    @Test
    void cancelStormSendsFortyCancelsAndOneFillForTheTrader() {
        int sent = simulator.cancelStorm("T-99");

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(publisher, times(82)).publish(captor.capture());
        List<OrderEvent> events = captor.getAllValues();

        assertThat(sent).isEqualTo(82);
        assertThat(events).allSatisfy(e -> assertThat(e.traderId()).isEqualTo("T-99"));
        assertThat(events.stream().filter(e -> e.action() == OrderEvent.Action.CANCEL)).hasSize(40);
        assertThat(events.stream().filter(e -> e.action() == OrderEvent.Action.FILL)).hasSize(1);
        assertThat(events.stream().filter(e -> e.action() == OrderEvent.Action.NEW)).hasSize(41);
    }

    @Test
    void spoofingStartsAtTheNextWindowBoundary() {
        long startsInMs = simulator.spoofing("T-77", "RY.TO");

        // next 10-second boundary is 0 to 10 s away, plus the 200 ms safety margin
        assertThat(startsInMs).isBetween(200L, 10_200L);
        // the events themselves are sent later by the scheduler; tearDown() cancels them
    }
}