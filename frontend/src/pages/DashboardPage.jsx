import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { palletizationApi } from '../services/api';
import StatusBadge from '../components/common/StatusBadge';
import Loading from '../components/common/Loading';

function DashboardPage() {
  const [executions, setExecutions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    palletizationApi.list({ page: 0, size: 10, sort: 'startedAt,desc' })
      .then(res => setExecutions(res.data.content || []))
      .catch(() => setExecutions([]))
      .finally(() => setLoading(false));
  }, []);

  const stats = {
    total: executions.length,
    completed: executions.filter(e => e.status === 'COMPLETED').length,
    errors: executions.filter(e => e.status === 'ERROR').length,
    avgPallets: executions.filter(e => e.totalPallets)
      .reduce((sum, e) => sum + e.totalPallets, 0) / Math.max(1, executions.filter(e => e.totalPallets).length),
  };

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard</h1>
        <Link to="/palletization/launch" className="btn btn-primary">
          Nouvelle palettisation
        </Link>
      </div>

      <div className="grid-4" style={{ marginBottom: '24px' }}>
        {[
          { label: 'Commandes', value: stats.total, color: 'var(--primary)' },
          { label: 'Palettes/commande', value: stats.avgPallets.toFixed(1), color: 'var(--temp-ambiant)' },
          { label: 'Terminées', value: stats.completed, color: 'var(--success)' },
          { label: 'Erreurs', value: stats.errors, color: 'var(--danger)' },
        ].map(s => (
          <div key={s.label} className="card" style={{ padding: '20px' }}>
            <div style={{ fontSize: '28px', fontWeight: '700', color: s.color }}>{s.value}</div>
            <div style={{ color: 'var(--gray-500)', marginTop: '4px' }}>{s.label}</div>
          </div>
        ))}
      </div>

      <div className="card">
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--gray-200)', fontWeight: '600' }}>
          Dernieres executions
        </div>
        {loading ? <Loading /> : (
          <table>
            <thead>
              <tr>
                <th>Execution</th>
                <th>Commande</th>
                <th>Statut</th>
                <th>Palettes</th>
                <th>Score</th>
                <th>Duree</th>
              </tr>
            </thead>
            <tbody>
              {executions.length === 0 ? (
                <tr><td colSpan="6" style={{ textAlign: 'center', color: 'var(--gray-500)' }}>Aucune execution</td></tr>
              ) : executions.map(e => (
                <tr key={e.executionId}>
                  <td>
                    <Link to={`/palletization/${e.executionId}`}>{e.executionId}</Link>
                  </td>
                  <td>{e.externalOrderId}</td>
                  <td><StatusBadge status={e.status} /></td>
                  <td>{e.totalPallets ?? '-'}</td>
                  <td>{e.globalScore != null ? `${e.globalScore}` : '-'}</td>
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

export default DashboardPage;
