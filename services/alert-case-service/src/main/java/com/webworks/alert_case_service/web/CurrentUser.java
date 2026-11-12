package com.webworks.alert_case_service.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public String name() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof Jwt jwt && jwt.getClaimAsString("preferred_username") != null) {
            return jwt.getClaimAsString("preferred_username");   // "analyst1", not a long id
        }
        return auth.getName();
    }

    public boolean isSupervisor() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"));
    }
}