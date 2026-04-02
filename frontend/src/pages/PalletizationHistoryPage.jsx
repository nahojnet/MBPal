import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { palletizationApi } from '../services/api';
import StatusBadge from '../components/common/StatusBadge';
import Loading from '../components/common/Loading';

function PalletizationHistoryPage() {
  const [executions, setExecutions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ orderId: '', customerId: '', status: '', page: 0 });

  const loadData = () => {
    setLoading(true);
    const params = { page: filters.page, size: 20 };
    if (filters.orderId) params.orderId = filters.orderId;
    if (filters.customerId) params.customerId = filters.customerId;
    if (filters.status) params.status = filters.status;
    palletizationApi.list(params)
      .then(res => setExecutions(res.data.content || []))
      .catch(() => setExecutions([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, [filters.page]);

  return (
    <div>
      <div className="page-header"><h1>Historique des executions</h1></div>

      <div className="card" style={{ padding: '16px', marginBottom: '16px' }}>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'end' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Commande</label>
            <input value={filters.orderId} onChange={e => setFilters(f => ({ ...f, orderId: e.target.value }))} placeholder="CMD-..." />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Client</label>
            <input value={filters.customerId} onChange={e => setFilters(f => ({ ...f, customerId: e.target.value }))} />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>Statut</label>
            <select value={filters.status} onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}>
              <option value="">Tous</option>
              <option value="COMPLETED">Termine</option>
              <option value="ERROR">Erreur</option>
              <option value="PROCESSING">En cours</option>
              <option value="PENDING">En attente</option>
            </select>
          </div>
          <button className="btn btn-primary" onClick={loadData}>Rechercher</button>
        </div>
      </div>

      <div className="card">
        {loading ? <Loading /> : (
          <table>
            <thead>
              <tr>
                <th>Execution</th><th>Commande</th><th>Client</th><th>Statut</th><th>Palettes</th><th>Score</th><th>Duree</th>
              </tr>
            </thead>
            <tbody>
              {executions.length === 0 ? (
                <tr><td colSpan="7" style={{ textAlign: 'center', color: 'var(--gray-500)' }}>Aucun resultat</td></tr>
              ) : executions.map(e => (
                <tr key={e.executionId}>
                  <td><Link to={`/palletization/${e.executionId}`}>{e.executionId}</Link></td>
                  <td>{e.externalOrderId}</td>
                  <td>{e.customerId}</td>
                  <td><StatusBadge status={e.status} /></td>
                  <td>{e.totalPallets ?? '-'}</td>
                  <td>{e.globalScore != null ? e.globalScore : '-'}</td>
                  <td>{e.durationMs != null ? `${(e.durationMs/1000).toFixed(1)}s` : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default PalletizationHistoryPage;
