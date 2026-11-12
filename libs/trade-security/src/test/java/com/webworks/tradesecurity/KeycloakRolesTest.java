package com.webworks.tradesecurity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesTest {

    private Jwt token(Map<String, Object> claims) {
        var b = Jwt.withTokenValue("t").header("alg", "none").subject("u");
        claims.forEach(b::claim);
        return b.build();
    }

    @Test
    void mapsRealmRolesToSpringRoles() {
        var jwt = token(Map.of("realm_access", Map.of("roles", List.of("ANALYST", "SUPERVISOR"))));
        assertThat(KeycloakRoles.from(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ANALYST", "ROLE_SUPERVISOR");
    }

    @Test
    void noRolesClaimMeansNoAuthorities() {
        assertThat(KeycloakRoles.from(token(Map.of("scope", "openid")))).isEmpty();
    }
}