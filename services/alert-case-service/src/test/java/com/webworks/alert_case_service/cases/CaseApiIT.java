package com.webworks.alert_case_service.cases;

import com.jayway.jsonpath.JsonPath;
import com.webworks.alert_case_service.TestcontainersConfig;
import com.webworks.alert_case_service.alert.Alert;
import com.webworks.alert_case_service.alert.AlertRepository;
import com.webworks.tradecommon.AlertEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CaseApiIT {

    @Autowired MockMvc mvc;
    @Autowired AlertRepository alerts;

    static RequestPostProcessor analyst() {
        return jwt().jwt(j -> j.claim("preferred_username", "analyst1"))
                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"));
    }

    static RequestPostProcessor supervisor() {
        return jwt().jwt(j -> j.claim("preferred_username", "supervisor1"))
                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"), new SimpleGrantedAuthority("ROLE_SUPERVISOR"));
    }

    private UUID newAlert() {
        Instant start = Instant.parse("2026-10-27T14:00:00Z");
        String key = UUID.randomUUID().toString().replace("-", "");
        return alerts.save(Alert.from(new AlertEvent(key, "SPOOFING", "T-77", "RY.TO", 80,
                start, start.plusSeconds(10), Map.of()))).getId();
    }

    /** Opens a case as analyst1 and returns its id. */
    private String openCase() throws Exception {
        String body = "{\"title\":\"Spoofing T-77\",\"alertIds\":[\"" + newAlert() + "\"]}";
        String json = mvc.perform(post("/api/cases").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.summary.id");
    }

    private int patchCase(String id, String body, RequestPostProcessor who) throws Exception {
        return mvc.perform(patch("/api/cases/" + id).with(who)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void analystCannotCloseButSupervisorCanAndEverythingIsAudited() throws Exception {
        String id = openCase();

        assertThat(patchCase(id, "{\"status\":\"IN_REVIEW\",\"version\":0}", analyst())).isEqualTo(200);
        assertThat(patchCase(id, "{\"status\":\"CLOSED\",\"version\":1}", analyst())).isEqualTo(403);
        assertThat(patchCase(id, "{\"status\":\"CLOSED\",\"version\":1}", supervisor())).isEqualTo(200);

        mvc.perform(get("/api/cases/" + id + "/events").with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].action").value("CASE_OPENED"))
                .andExpect(jsonPath("$[0].actor").value("analyst1"))
                .andExpect(jsonPath("$[1].detail.to").value("IN_REVIEW"))
                .andExpect(jsonPath("$[2].actor").value("supervisor1"))
                .andExpect(jsonPath("$[2].detail.to").value("CLOSED"));
    }

    @Test
    void skippingAStepGives409() throws Exception {
        String id = openCase();
        assertThat(patchCase(id, "{\"status\":\"CLOSED\",\"version\":0}", supervisor())).isEqualTo(409);
    }

    @Test
    void twoUpdatesWithTheSameVersionGiveExactlyOne409() throws Exception {
        String id = openCase();
        String body = "{\"priority\":%d,\"version\":0}";

        var first = CompletableFuture.supplyAsync(() -> call(() -> patchCase(id, body.formatted(1), analyst())));
        var second = CompletableFuture.supplyAsync(() -> call(() -> patchCase(id, body.formatted(5), analyst())));

        assertThat(List.of(first.join(), second.join())).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    void blankTitleGives400() throws Exception {
        mvc.perform(post("/api/cases").with(analyst()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"alertIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    private interface Call { int run() throws Exception; }

    private static int call(Call c) {
        try {
            return c.run();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}