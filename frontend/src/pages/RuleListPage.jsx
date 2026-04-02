import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ruleApi } from '../services/api';
import StatusBadge from '../components/common/StatusBadge';
import Loading from '../components/common/Loading';

function RuleListPage() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ scope: '', severity: '' });

  useEffect(() => {
    setLoading(true);
    const params = {};
    if (filters.scope) params.scope = filters.scope;
    if (filters.severity) params.severity = filters.severity;
    ruleApi.list(params)
      .then(res => setRules(res.data.content || res.data || []))
      .catch(() => setRules([]))
      .finally(() => setLoading(false));
  }, [filters]);

  return (
    <div>
      <div className="page-header">
        <h1>Regles de palettisation</h1>
        <Link to="/rules/create" className="btn btn-primary">Creer une regle</Link>
      </div>

      <div className="card" style={{ padding: '12px 16px', marginBottom: '16px' }}>
        <div style={{ display: 'flex', gap: '12px' }}>
          <select value={filters.scope} onChange={e => setFilters(f => ({ ...f, scope: e.target.value }))}>
            <option value="">Toutes les portees</option>
            <option value="PACKAGE">Colis</option>
            <option value="PALLET">Palette</option>
            <option value="INTER_PACKAGE">Inter-colis</option>
          </select>
          <select value={filters.severity} onChange={e => setFilters(f => ({ ...f, severity: e.target.value }))}>
            <option value="">Toutes les severites</option>
            <option value="HARD">Obligatoire</option>
            <option value="SOFT">Preferentielle</option>
          </select>
        </div>
      </div>

      <div className="card">
        {loading ? <Loading /> : (
          <table>
            <thead>
              <tr><th>Code</th><th>Portee</th><th>Severite</th><th>Description</th><th>Derniere version</th></tr>
            </thead>
            <tbody>
              {rules.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', color: 'var(--gray-500)' }}>Aucune regle</td></tr>
              ) : rules.map(r => (
                <tr key={r.ruleCode}>
                  <td><strong>{r.ruleCode}</strong></td>
                  <td><span className="badge" style={{ background: 'var(--gray-100)' }}>{r.scope}</span></td>
                  <td>
                    <span className="badge" style={{
                      background: r.severity === 'HARD' ? 'var(--danger-light)' : 'var(--warning-light)',
                      color: r.severity === 'HARD' ? 'var(--danger)' : 'var(--warning)'
                    }}>{r.severity}</span>
                  </td>
                  <td>{r.description}</td>
                  <td>{r.latestVersion || '-'} {r.latestVersionStatus && <StatusBadge status={r.latestVersionStatus} />}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default RuleListPage;
