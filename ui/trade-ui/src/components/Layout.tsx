import { useCallback, useState } from 'react';
import { Button, Container, Nav, Navbar, Toast, ToastContainer } from 'react-bootstrap';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { useAlertStream } from '../hooks/useAlertStream';
import type { AlertCreated } from '../types';

export default function Layout() {
    const { username, isSupervisor, logout } = useAuth();
    const queryClient = useQueryClient();
    const [toasts, setToasts] = useState<AlertCreated[]>([]);

    // useCallback keeps the same function between renders, so the stream is not reopened each time
    const onAlert = useCallback((alert: AlertCreated) => {
        setToasts((current) => [alert, ...current].slice(0, 5));       // show at most 5
        queryClient.invalidateQueries({ queryKey: ['alerts'] });          // the table refetches
    }, [queryClient]);
    useAlertStream(onAlert);

    const dismiss = (id: string) => setToasts((current) => current.filter((t) => t.alertId !== id));

    return (
        <>
            <Navbar bg="dark" data-bs-theme="dark" expand="md" className="mb-4">
                <Container>
                    <Navbar.Brand as={Link} to="/alerts">Trade Surveillance</Navbar.Brand>
                    <Navbar.Toggle aria-controls="main-nav" />
                    <Navbar.Collapse id="main-nav">
                        <Nav className="me-auto">
                            <Nav.Link as={NavLink} to="/alerts">Alerts</Nav.Link>
                            <Nav.Link as={NavLink} to="/cases">Cases</Nav.Link>
                            {isSupervisor && <Nav.Link as={NavLink} to="/simulator">Simulator</Nav.Link>}
                        </Nav>
                        <Navbar.Text className="me-3">
                            Signed in as <strong>{username}</strong>{isSupervisor ? ' (supervisor)' : ''}
                        </Navbar.Text>
                        <Button variant="outline-light" size="sm" onClick={logout}>Log out</Button>
                    </Navbar.Collapse>
                </Container>
            </Navbar>

            <Container className="pb-5">
                <Outlet />
            </Container>

            <ToastContainer position="bottom-end" className="p-3">
                {toasts.map((a) => (
                    <Toast key={a.alertId} onClose={() => dismiss(a.alertId)} delay={8000} autohide>
                        <Toast.Header>
                            <strong className="me-auto">New {a.rule} alert</strong>
                        </Toast.Header>
                        <Toast.Body>
                            Trader {a.traderId}{a.symbol ? ` in ${a.symbol}` : ''}.{' '}
                            <Link to={`/alerts/${a.alertId}`} onClick={() => dismiss(a.alertId)}>Open alert</Link>
                        </Toast.Body>
                    </Toast>
                ))}
            </ToastContainer>
        </>
    );
}