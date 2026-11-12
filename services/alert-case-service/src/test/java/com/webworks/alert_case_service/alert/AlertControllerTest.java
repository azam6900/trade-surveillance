package com.webworks.alert_case_service.alert;

import com.webworks.alert_case_service.web.NotFoundException;
import com.webworks.alert_case_service.web.PageResponse;
import com.webworks.tradesecurity.TradeSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@Import(TradeSecurityConfig.class)
class AlertControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean
    AlertQueryService query;   // a Mockito fake placed in the Spring context

    @Test
    void analystCanListAlerts() throws Exception {
        when(query.search(any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/alerts?rule=SPOOFING").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void userWithoutARoleGets403() throws Exception {
        mvc.perform(get("/api/alerts").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousGets401() throws Exception {
        mvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownAlertGets404() throws Exception {
        UUID id = UUID.randomUUID();
        when(query.get(id)).thenThrow(new NotFoundException("Alert " + id + " not found"));

        mvc.perform(get("/api/alerts/" + id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isNotFound());
    }
}