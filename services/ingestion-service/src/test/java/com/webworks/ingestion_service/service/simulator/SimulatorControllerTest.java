package com.webworks.ingestion_service.service.simulator;

import com.webworks.ingestion_service.simulator.Simulator;
import com.webworks.ingestion_service.simulator.SimulatorController;
import com.webworks.tradesecurity.TradeSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulatorController.class)
@Import(TradeSecurityConfig.class)
class SimulatorControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean
    Simulator simulator;

    @Test
    void analystGets403() throws Exception {
        mvc.perform(post("/api/simulate/cancel-storm").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorGets200() throws Exception {
        mvc.perform(post("/api/simulate/cancel-storm").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isOk());
    }
}