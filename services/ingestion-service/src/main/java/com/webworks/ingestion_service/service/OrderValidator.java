package com.webworks.ingestion_service.service;

import com.webworks.tradecommon.OrderEvent;

import java.util.ArrayList;
import java.util.List;

public final class OrderValidator {

    private OrderValidator() {
    }

    public static List<String> validate(OrderEvent e) {
        List<String> errors = new ArrayList<>();
        if (e == null) {
            errors.add("event is missing");
            return errors;
        }
        if (isBlank(e.orderId())) errors.add("orderId is required");
        if (isBlank(e.traderId())) errors.add("traderId is required");
        if (isBlank(e.symbol())) errors.add("symbol is required");
        if (e.side() == null) errors.add("side is required");
        if (e.action() == null) errors.add("action is required");
        if (e.ts() == null) errors.add("ts is required");
        if (e.qty() <= 0) errors.add("qty must be positive");
        if (e.type() == OrderEvent.OrderType.LIMIT && e.action() == OrderEvent.Action.NEW
                && (e.price() == null || e.price().signum() <= 0)) {
            errors.add("price must be positive for a LIMIT order");
        }
        return errors;
    }

    public static boolean isValid(OrderEvent e) {
        return validate(e).isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}