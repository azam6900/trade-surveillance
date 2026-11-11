package com.webworks.ingestion_service.simulator;

import com.webworks.ingestion_service.service.OrderPublisher;
import com.webworks.tradecommon.OrderEvent;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Simulator {

    private static final List<String> SYMBOLS = List.of("RY.TO", "TD.TO", "BNS.TO", "BMO.TO", "CM.TO",
            "ENB.TO", "SHOP.TO", "CNQ.TO", "SU.TO", "TRP.TO");

    private final OrderPublisher publisher;
    private volatile int eventsPerSecond;
    private final Random random = new Random(42);                    // seeded: repeatable runs
    private final Deque<OrderEvent> open = new ArrayDeque<>();        // orders that can still be cancelled or filled
    private final AtomicInteger ids = new AtomicInteger();
    private final String runId = Long.toString(System.currentTimeMillis(), 36);   // unique order IDs per run
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public Simulator(OrderPublisher publisher,
                     @Value("${simulator.events-per-second:20}") int eventsPerSecond) {
        this.publisher = publisher;
        this.eventsPerSecond = eventsPerSecond;
    }

    public void setEventsPerSecond(int eventsPerSecond) {
        this.eventsPerSecond = eventsPerSecond;
    }

    public int getEventsPerSecond() {
        return eventsPerSecond;
    }

    // ---- 1. background traffic ----------------------------------------------

    @Scheduled(fixedRate = 1000)
    void tick() {
        for (int i = 0; i < eventsPerSecond; i++) {
            publisher.publish(nextRandomEvent());
        }
    }

    private OrderEvent nextRandomEvent() {
        synchronized (open) {
            int roll = random.nextInt(100);
            if (roll >= 60 && !open.isEmpty()) {
                OrderEvent existing = open.pollFirst();
                // 25% cancels, 15% fills: normal traders cancel a bit more than they fill, never 10 to 1
                return withAction(existing, roll < 85 ? OrderEvent.Action.CANCEL : OrderEvent.Action.FILL);
            }
            OrderEvent created = newOrder(randomTrader(), SYMBOLS.get(random.nextInt(SYMBOLS.size())),
                    random.nextBoolean() ? OrderEvent.Side.BUY : OrderEvent.Side.SELL,
                    100L * (1 + random.nextInt(9)), randomPrice());   // 100 to 900 shares: never "large"
            open.addLast(created);
            if (open.size() > 5000) open.pollFirst();               // keep memory bounded
            return created;
        }
    }

    // ---- 2. scenarios --------------------------------------------------------

    /** One trader places and cancels 40 orders and fills almost none: should trigger CANCEL_RATIO. */
    public int cancelStorm(String traderId) {
        int sent = 0;
        for (int i = 0; i < 40; i++) {
            OrderEvent order = newOrder(traderId, "RY.TO", OrderEvent.Side.BUY, 100, randomPrice());
            publisher.publish(order);
            publisher.publish(withAction(order, OrderEvent.Action.CANCEL));
            sent += 2;
        }
        OrderEvent filled = newOrder(traderId, "RY.TO", OrderEvent.Side.BUY, 100, randomPrice());
        publisher.publish(filled);
        publisher.publish(withAction(filled, OrderEvent.Action.FILL));
        return sent + 2;   // 40 x (new + cancel) + (new + fill) = 82 events
    }

    /**
     * Spoofing: five big BUY orders, a small SELL that fills, then the big orders are cancelled.
     * The detection window is 10 seconds and aligned to the clock, so we wait for the next
     * window boundary first; otherwise the pattern could be split across two windows and missed.
     */
    public long spoofing(String traderId, String symbol) {
        long start = millisToNextBoundary(10_000) + 200;
        List<OrderEvent> layers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            layers.add(newOrder(traderId, symbol, OrderEvent.Side.BUY, 10_000, BigDecimal.valueOf(14_000 + 5L * i, 2)));
        }
        OrderEvent sell = newOrder(traderId, symbol, OrderEvent.Side.SELL, 500, BigDecimal.valueOf(14_030, 2));

        for (int i = 0; i < 5; i++) send(start + 200L * i, layers.get(i));          // 0.0 to 0.8 s
        send(start + 1500, sell);                                                   // 1.5 s
        send(start + 1700, withAction(sell, OrderEvent.Action.FILL));               // 1.7 s
        for (int i = 0; i < 5; i++) {
            send(start + 3000 + 200L * i, withAction(layers.get(i), OrderEvent.Action.CANCEL));   // 3.0 to 3.8 s
        }
        return start;
    }

    // ---- helpers -------------------------------------------------------------

    private void send(long delayMs, OrderEvent event) {
        // re-stamp with the moment it is actually sent, so event time matches the schedule
        scheduler.schedule(() -> publisher.publish(withAction(event, event.action())), delayMs, TimeUnit.MILLISECONDS);
    }

    private static long millisToNextBoundary(long windowMs) {
        return windowMs - (System.currentTimeMillis() % windowMs);
    }

    private OrderEvent newOrder(String trader, String symbol, OrderEvent.Side side, long qty, BigDecimal price) {
        return new OrderEvent("O-" + runId + "-" + ids.incrementAndGet(), trader, symbol, side,
                OrderEvent.OrderType.LIMIT, price, qty, OrderEvent.Action.NEW, Instant.now());
    }

    /** Same order, new action, timestamp set to "now". */
    private static OrderEvent withAction(OrderEvent o, OrderEvent.Action action) {
        return new OrderEvent(o.orderId(), o.traderId(), o.symbol(), o.side(), o.type(), o.price(), o.qty(),
                action, Instant.now());
    }

    private String randomTrader() {
        return String.format("T-%02d", 1 + random.nextInt(20));   // T-01 to T-20; scenarios use T-77 and T-99
    }

    private BigDecimal randomPrice() {
        return BigDecimal.valueOf(10_000 + random.nextInt(10_000), 2);   // 100.00 to 199.99
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}