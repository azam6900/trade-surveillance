package com.webworks.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest
@Testcontainers
class GatewayIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    /** A fake backend that answers every request with an empty page of alerts. */
    static final HttpServer backend = startBackend();

    static HttpServer startBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", exchange -> {
                byte[] body = "{\"content\":[]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void routesToFakeBackend(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + backend.getAddress().getPort();
        registry.add("ALERT_CASE_URL", () -> url);
        registry.add("INGESTION_URL", () -> url);
        registry.add("RATE_LIMIT_PER_SECOND", () -> "1");
        registry.add("RATE_LIMIT_BURST", () -> "2");
    }

    @Autowired ApplicationContext context;
    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context).apply(springSecurity()).configureClient().build();
    }

    @Test
    void noTokenGives401() {
        client.get().uri("/api/alerts").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void validTokenReachesTheBackend() {
        client.mutateWith(mockJwt().jwt(j -> j.subject("route-user")))
                .get().uri("/api/alerts").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content").isArray();
    }

    @Test
    void burstOfRequestsFromOneUserIsLimited() {
        int limited = 0;
        for (int i = 0; i < 5; i++) {
            int code = client.mutateWith(mockJwt().jwt(j -> j.subject("busy-user")))
                    .get().uri("/api/alerts").exchange()
                    .returnResult(String.class).getStatus().value();
            if (code == 429) limited++;
        }
        org.assertj.core.api.Assertions.assertThat(limited).isGreaterThanOrEqualTo(1);
    }
}