package com.webworks.tradecommon;

public final class Topics {

    private Topics() {
    }

    public static final String ORDERS = "orders.events";
    public static final String ORDERS_INVALID = "orders.invalid";
    public static final String ALERTS = "alerts.raw";
    public static final String ALERTS_DLT = "alerts.raw.DLT";   // used in Step 3.7
}