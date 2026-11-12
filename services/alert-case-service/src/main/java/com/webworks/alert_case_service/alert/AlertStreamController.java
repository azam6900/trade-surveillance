package com.webworks.alert_case_service.alert;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@RestController
public class AlertStreamController {

    // CopyOnWriteArrayList: safe to loop over while other threads add and remove clients
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(path = "/api/stream/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());   // client reconnects after 30 min
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    /** Published by AlertIngestService (Step 3.6); runs only after that transaction commits. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertCreated(AlertCreated alert) {
        sendToAll(() -> SseEmitter.event().name("alert").data(alert, MediaType.APPLICATION_JSON));
    }

    /** A comment line every 15 s, so proxies and the gateway do not close an idle connection. */
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        sendToAll(() -> SseEmitter.event().comment("keep-alive"));
    }

    private void sendToAll(Supplier<SseEmitter.SseEventBuilder> event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event.get());
            } catch (Exception e) {          // the browser went away
                emitters.remove(emitter);
            }
        }
    }
}