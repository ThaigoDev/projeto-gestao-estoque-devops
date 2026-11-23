import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { DashboardPage } from './pages/DashboardPage';
import { EntradasPage } from './pages/EntradasPage';
import { NovaVendaPage } from './pages/NovaVendaPage';
import { ProdutosPage } from './pages/ProdutosPage';
import { RelatoriosPage } from './pages/RelatoriosPage';
import { ReposicoesPage } from './pages/ReposicoesPage';
import { VendasPage } from './pages/VendasPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="produtos" element={<ProdutosPage />} />
        <Route path="entradas" element={<EntradasPage />} />
        <Route path="vendas" element={<VendasPage />} />
        <Route path="vendas/nova" element={<NovaVendaPage />} />
        <Route path="reposicoes" element={<ReposicoesPage />} />
        <Route path="relatorios" element={<RelatoriosPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
