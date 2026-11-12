package com.webworks.detection_service.config;

import com.webworks.tradesecurity.TradeSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TradeSecurityConfig.class)
public class SecurityConfig {
}