import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 },   // ramp up to 20 virtual users
        { duration: '1m', target: 50 },    // then to 50, and hold
        { duration: '30s', target: 0 },    // ramp down
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],     // fewer than 1% errors
        http_req_duration: ['p(95)<300'],   // 95% of requests under 300 ms
    },
};

// Runs once before the test: log in and share the token with every virtual user.
export function setup() {
    const res = http.post('http://localhost:8180/realms/trade/protocol/openid-connect/token', {
        client_id: 'trade-ui', grant_type: 'password', username: 'analyst1', password: __ENV.PASS,
    });
    return { token: res.json('access_token') };
}

export default function (data) {
    const res = http.get('http://localhost:8080/api/alerts?size=20', {
        headers: { Authorization: `Bearer ${data.token}` },
    });
    check(res, { 'status is 200 or 429': (r) => r.status === 200 || r.status === 429 });
    sleep(0.2);
}