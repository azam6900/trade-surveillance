package com.webworks.ingestion_service.simulator;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simulate")
@PreAuthorize("hasRole('SUPERVISOR')")
public class SimulatorController {

    private final Simulator simulator;

    public SimulatorController(Simulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/cancel-storm")
    public Map<String, Object> cancelStorm(@RequestParam(defaultValue = "T-99") String traderId) {
        int events = simulator.cancelStorm(traderId);
        return Map.of("scenario", "cancel-storm", "traderId", traderId, "eventsSent", events);
    }

    @PostMapping("/spoofing")
    public Map<String, Object> spoofing(@RequestParam(defaultValue = "T-77") String traderId,
                                        @RequestParam(defaultValue = "RY.TO") String symbol) {
        long startsInMs = simulator.spoofing(traderId, symbol);
        return Map.of("scenario", "spoofing", "traderId", traderId, "symbol", symbol, "startsInMs", startsInMs);
    }
}