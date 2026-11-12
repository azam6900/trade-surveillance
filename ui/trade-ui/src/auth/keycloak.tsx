import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL,
    realm: 'trade',
    clientId: 'trade-ui',
});

let started: Promise<boolean> | undefined;

/** Starts the login once, however many times it is called. */
export function startAuth(): Promise<boolean> {
    started ??= keycloak.init({
        onLoad: 'login-required',   // not logged in? go to Keycloak's login page
        pkceMethod: 'S256',
        checkLoginIframe: false,    // the hidden iframe check breaks with modern third-party cookie rules
    });
    return started;
}