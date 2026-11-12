import { useQuery } from '@tanstack/react-query';
import { Alert, Spinner } from 'react-bootstrap';
import { api } from '../api/client';
import CaseBoard from '../components/CaseBoard';
import type { CaseSummary, Page } from '../types';
import { usePageTitle } from '../hooks/usePageTitle';

export default function CasesPage() {
    usePageTitle('Alerts');
    const { data, isPending, error } = useQuery({
        queryKey: ['cases'],
        queryFn: () => api<Page<CaseSummary>>('/api/cases?size=200'),
    });
    return (
        <>
            <h1 className="h4 mb-3">Cases</h1>
            {isPending && <Spinner animation="border" role="status"><span className="visually-hidden">Loading cases</span></Spinner>}
            {error && <Alert variant="danger">Could not load cases: {error.message}</Alert>}
            {data && <CaseBoard cases={data.content} />}
        </>
    );
}