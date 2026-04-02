import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { rulesetApi } from '../services/api';
import StatusBadge from '../components/common/StatusBadge';
import Loading from '../components/common/Loading';

function RulesetListPage() {
  const [rulesets, setRulesets] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = () => {
    setLoading(true);
    rulesetApi.list()
      .then(res => setRulesets(res.data.content || res.data || []))
      .catch(() => setRulesets([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  const handlePublish = async (code) => {
    try {
      await rulesetApi.publish(code);
      loadData();
    } catch (err) {
      alert(err.response?.data?.message || 'Erreur');
    }
  };

  return (
    <div>
      <div className="page-header"><h1>Rulesets</h1></div>

      <div className="card">
        {loading ? <Loading /> : (
          <table>
            <thead>
              <tr><th>Code</th><th>Label</th><th>Statut</th><th>Publie le</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {rulesets.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', color: 'var(--gray-500)' }}>Aucun ruleset</td></tr>
              ) : rulesets.map(r => (
                <tr key={r.rulesetCode}>
                  <td><strong>{r.rulesetCode}</strong></td>
                  <td>{r.label}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>{r.publishedAt ? new Date(r.publishedAt).toLocaleDateString('fr') : '-'}</td>
                  <td style={{ display: 'flex', gap: '6px' }}>
                    <Link to={`/rulesets/${r.rulesetCode}/priorities`} className="btn btn-outline" style={{ padding: '4px 10px', fontSize: '12px' }}>Priorites</Link>
                    {r.status === 'DRAFT' && (
                      <button className="btn btn-success" style={{ padding: '4px 10px', fontSize: '12px' }} onClick={() => handlePublish(r.rulesetCode)}>Publier</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default RulesetListPage;
