package com.webworks.detection_service.rules;

import com.webworks.tradecommon.OrderEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpoofState {

    public static final long LARGE_QTY = 1_000;        // orders of 1,000 shares or more count as "large"
    public static final double CANCEL_SHARE = 0.9;     // 90% of the large quantity must be cancelled
    private static final int MAX_EVIDENCE = 50;        // keep the stored state small

    public String traderId;
    public String symbol;
    public OrderEvent.Side layerSide;                  // side of the first large order (the "layer")
    public long placedQty;                             // large quantity placed on the layer side
    public long cancelledQty;                          // large quantity cancelled on the layer side
    public boolean oppositeFill;                       // a trade happened on the other side
    public boolean alerted;                            // already fired in this window
    public boolean emit;                               // fired on THIS event
    public Instant lastTs;
    public List<Map<String, Object>> evidence = new ArrayList<>();

    public SpoofState apply(OrderEvent e) {
        emit = false;
        traderId = e.traderId();
        symbol = e.symbol();
        lastTs = e.ts();
        boolean large = e.qty() >= LARGE_QTY;

        switch (e.action()) {
            case NEW -> {
                if (large) {
                    if (layerSide == null) layerSide = e.side();
                    if (e.side() == layerSide) placedQty += e.qty();
                }
            }
            case FILL -> {
                if (layerSide != null && e.side() != layerSide) oppositeFill = true;
            }
            case CANCEL -> {
                if (large && e.side() == layerSide) cancelledQty += e.qty();
            }
        }

        if (evidence.size() < MAX_EVIDENCE) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderId", e.orderId());
            item.put("action", e.action().name());
            item.put("side", e.side().name());
            item.put("qty", e.qty());
            item.put("price", String.valueOf(e.price()));
            item.put("ts", e.ts().toString());
            evidence.add(item);
        }

        if (!alerted && oppositeFill && placedQty > 0 && cancelledQty >= CANCEL_SHARE * placedQty) {
            alerted = true;
            emit = true;
        }
        return this;
    }
}