package com.webworks.detection_service.rules;

import com.webworks.tradecommon.OrderEvent;

import java.time.Instant;

public class OtrState {

    public long cancels;
    public long fills;
    public boolean alerted;   // already fired in this window?
    public boolean emit;      // did it fire on THIS event?
    public Instant lastTs;

    public OtrState apply(OrderEvent e, int minCancels, double ratio) {
        emit = false;
        lastTs = e.ts();
        if (e.action() == OrderEvent.Action.CANCEL) cancels++;
        if (e.action() == OrderEvent.Action.FILL) fills++;
        if (!alerted && cancels >= minCancels && (double) cancels / Math.max(fills, 1) >= ratio) {
            alerted = true;   // fire once per window
            emit = true;
        }
        return this;
    }
}