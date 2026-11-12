package com.webworks.tradesecurity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Keycloak puts roles under realm_access.roles; Spring wants authorities named ROLE_<name>. */
public final class KeycloakRoles {

    private KeycloakRoles() {
    }

    public static Collection<GrantedAuthority> from(Jwt jwt) {
        Map<String, Object> realm = jwt.getClaim("realm_access");
        if (realm == null || !(realm.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }
}