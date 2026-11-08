package com.webworks.tradecommon;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderEvent(String orderId, String traderId, String symbol,
                         Side side, OrderType type, BigDecimal price,
                         long qty, Action action, Instant ts) {
    public enum Side { BUY, SELL }
    public enum OrderType { LIMIT, MARKET }
    public enum Action { NEW, CANCEL, FILL }
}