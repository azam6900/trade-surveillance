package com.webworks.tradecommon;

import java.time.Instant;
import java.util.Map;

public record AlertEvent(String alertKey, String rule, String traderId,
                         String symbol, int severity, Instant windowStart,
                         Instant windowEnd, Map<String, Object> evidence) {}