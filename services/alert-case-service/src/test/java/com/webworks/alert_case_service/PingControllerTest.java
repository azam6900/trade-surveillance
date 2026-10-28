package com.webworks.alert_case_service;

import com.webworks.alert_case_service.config.SecurityConfig;
import com.webworks.alert_case_service.controller.PingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PingController.class) @Import(SecurityConfig.class)
public class PingControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder; // <-- This satisfies the missing bean requirement

    @Test
    void anonymousGets401() throws Exception {
        mvc.perform(get("/api/ping")).andExpect(status().isUnauthorized());
    }
    @Test void analystGets200() throws Exception {
        mvc.perform(get("/api/ping").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk());
    }
}