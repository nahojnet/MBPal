import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/layout/Layout';
import DashboardPage from './pages/DashboardPage';
import PalletizationLaunchPage from './pages/PalletizationLaunchPage';
import PalletizationHistoryPage from './pages/PalletizationHistoryPage';
import PalletizationResultPage from './pages/PalletizationResultPage';
import PalletizationComparePage from './pages/PalletizationComparePage';
import RuleListPage from './pages/RuleListPage';
import RuleCreatePage from './pages/RuleCreatePage';
import RulesetListPage from './pages/RulesetListPage';
import RulesetPriorityPage from './pages/RulesetPriorityPage';
import ProductListPage from './pages/ProductListPage';
import SupportListPage from './pages/SupportListPage';

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/palletization/launch" element={<PalletizationLaunchPage />} />
          <Route path="/palletization/history" element={<PalletizationHistoryPage />} />
          <Route path="/palletization/:executionId" element={<PalletizationResultPage />} />
          <Route path="/palletization/compare" element={<PalletizationComparePage />} />
          <Route path="/rules" element={<RuleListPage />} />
          <Route path="/rules/create" element={<RuleCreatePage />} />
          <Route path="/rulesets" element={<RulesetListPage />} />
          <Route path="/rulesets/:rulesetCode/priorities" element={<RulesetPriorityPage />} />
          <Route path="/referentials/products" element={<ProductListPage />} />
          <Route path="/referentials/supports" element={<SupportListPage />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
