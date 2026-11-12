import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.tsx';
import RequireRole from './components/RequireRole.tsx';
import AlertsPage from './pages/AlertsPage';
import AlertDetailPage from './pages/AlertDetailPage';
import CasesPage from './pages/CasesPage';
import SimulatorPage from './pages/SimulatorPage';
import CaseDetailPage from './pages/CaseDetailPage';

export default function App() {
  return (
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/alerts" replace />} />
          <Route path="alerts" element={<AlertsPage />} />
          <Route path="alerts/:id" element={<AlertDetailPage />} />
          <Route path="cases" element={<CasesPage />} />
          <Route path="cases/:id" element={<CaseDetailPage />} />
            <Route path="simulator" element={<RequireRole role="SUPERVISOR"><SimulatorPage /></RequireRole>} />
          <Route path="*" element={<p>Page not found.</p>} />
        </Route>
      </Routes>
  );
}