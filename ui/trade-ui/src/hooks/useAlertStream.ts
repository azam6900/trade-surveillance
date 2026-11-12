import { useEffect, useRef } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { API_URL } from '../api/client';
import { keycloak } from '../auth/keycloak';
import type { AlertCreated } from '../types';

export function useAlertStream(onAlert: (a: AlertCreated) => void): void {
    const handler = useRef(onAlert);
    useEffect(() => {
        handler.current = onAlert;            // always call the latest callback
    }, [onAlert]);

    useEffect(() => {
        let stopped = false;
        let controller: AbortController | undefined;

        const connect = async () => {
            if (stopped) return;
            await keycloak.updateToken(30).catch(() => keycloak.login());
            controller = new AbortController();
            try {
                await fetchEventSource(`${API_URL}/api/stream/alerts`, {
                    headers: { Authorization: `Bearer ${keycloak.token}` },
                    signal: controller.signal,
                    openWhenHidden: true,                       // keep streaming when the tab is in the background
                    onmessage(msg) {
                        if (msg.event === 'alert') handler.current(JSON.parse(msg.data) as AlertCreated);
                    },
                    onerror(err) {
                        throw err;                                // stop the library's retry; we reconnect below with a fresh token
                    },
                });
            } catch {
                // connection dropped or was refused; fall through to reconnect
            }
            if (!stopped) setTimeout(connect, 3000);
        };

        connect();
        return () => {                                    // the component unmounted: close the stream for good
            stopped = true;
            controller?.abort();
        };
    }, []);
}