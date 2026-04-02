import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { rulesetApi } from '../services/api';
import Loading from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';

function RulesetPriorityPage() {
  const { rulesetCode } = useParams();
  const [priorities, setPriorities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    rulesetApi.getPriorities(rulesetCode)
      .then(res => setPriorities(res.data.priorities || res.data || []))
      .catch(() => setPriorities([]))
      .finally(() => setLoading(false));
  }, [rulesetCode]);

  const moveUp = (i) => {
    if (i === 0) return;
    const newList = [...priorities];
    [newList[i - 1], newList[i]] = [newList[i], newList[i - 1]];
    newList.forEach((p, idx) => p.priorityOrder = idx + 1);
    setPriorities(newList);
    setSaved(false);
  };

  const moveDown = (i) => {
    if (i === priorities.length - 1) return;
    const newList = [...priorities];
    [newList[i], newList[i + 1]] = [newList[i + 1], newList[i]];
    newList.forEach((p, idx) => p.priorityOrder = idx + 1);
    setPriorities(newList);
    setSaved(false);
  };

  const updateWeight = (i, weight) => {
    setPriorities(prev => prev.map((p, idx) => idx === i ? { ...p, weight: Number(weight) } : p));
    setSaved(false);
  };

  const handleSave = async () => {
    setError('');
    try {
      await rulesetApi.updatePriorities(rulesetCode, {
        priorities: priorities.map(p => ({
          ruleCode: p.ruleCode,
          priorityOrder: p.priorityOrder,
          weight: p.weight,
        })),
      });
      setSaved(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors de la sauvegarde');
    }
  };

  if (loading) return <Loading />;

  return (
    <div>
      <div className="page-header">
        <h1>Priorites SOFT — {rulesetCode}</h1>
        <button className="btn btn-primary" onClick={handleSave}>Sauvegarder</button>
      </div>

      <ErrorMessage message={error} />
      {saved && <div style={{ background: 'var(--success-light)', color: 'var(--success)', padding: '12px', borderRadius: 'var(--radius)', marginBottom: '16px' }}>Priorites sauvegardees.</div>}

      <div className="card">
        <table>
          <thead>
            <tr><th>Ordre</th><th>Code regle</th><th>Poids (0-100)</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {priorities.length === 0 ? (
              <tr><td colSpan="4" style={{ textAlign: 'center', color: 'var(--gray-500)' }}>Aucune regle SOFT dans ce ruleset</td></tr>
            ) : priorities.map((p, i) => (
              <tr key={p.ruleCode}>
                <td><strong>{p.priorityOrder}</strong></td>
                <td>{p.ruleCode}</td>
                <td>
                  <input type="number" min="0" max="100" value={p.weight} onChange={e => updateWeight(i, e.target.value)} style={{ width: '80px' }} />
                </td>
                <td style={{ display: 'flex', gap: '4px' }}>
                  <button className="btn btn-outline" style={{ padding: '2px 8px' }} onClick={() => moveUp(i)} disabled={i === 0}>^</button>
                  <button className="btn btn-outline" style={{ padding: '2px 8px' }} onClick={() => moveDown(i)} disabled={i === priorities.length - 1}>v</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default RulesetPriorityPage;
