import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Alert, Button, Card, Form, Stack } from 'react-bootstrap';
import { api } from '../api/client';

type Scenario = 'cancel-storm' | 'spoofing';

export default function SimulatorPage() {
    const [traderId, setTraderId] = useState('T-77');
    const run = useMutation({
        mutationFn: (scenario: Scenario) =>
            api<Record<string, unknown>>(`/api/simulate/${scenario}?traderId=${encodeURIComponent(traderId)}`, { method: 'POST' }),
    });

    return (
        <Card>
            <Card.Body>
                <Card.Title as="h1" className="h4">Simulator</Card.Title>
                <Card.Text>Start a scenario. An alert should appear within about 15 seconds.</Card.Text>
                <Form.Group className="mb-3" controlId="sim-trader">
                    <Form.Label>Trader ID</Form.Label>
                    <Form.Control value={traderId} onChange={(e) => setTraderId(e.target.value)} />
                </Form.Group>
                <Stack direction="horizontal" gap={2}>
                    <Button onClick={() => run.mutate('cancel-storm')} disabled={run.isPending}>Cancel storm</Button>
                    <Button variant="warning" onClick={() => run.mutate('spoofing')} disabled={run.isPending}>Spoofing</Button>
                </Stack>
                {run.isSuccess && <Alert variant="success" className="mt-3">Scenario started: {JSON.stringify(run.data)}</Alert>}
                {run.isError && <Alert variant="danger" className="mt-3">Could not start it: {run.error.message}</Alert>}
            </Card.Body>
        </Card>
    );
}