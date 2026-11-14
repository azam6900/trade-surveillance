package com.webworks.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;

        ServerWebExchange withId = exchange.mutate().request(r -> r.header(HEADER, id)).build();
        withId.getResponse().getHeaders().set(HEADER, id);
        log.atInfo().addKeyValue("correlationId", id)
                .log("{} {}", exchange.getRequest().getMethod(), exchange.getRequest().getPath());
        return chain.filter(withId);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;   // run before routing
    }
}