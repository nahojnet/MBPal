import { useState } from 'react';
import { palletizationApi } from '../services/api';
import ErrorMessage from '../components/common/ErrorMessage';
import Loading from '../components/common/Loading';

function PalletizationComparePage() {
  const [id1, setId1] = useState('');
  const [id2, setId2] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCompare = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await palletizationApi.compare({ executionId1: id1, executionId2: id2 });
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors de la comparaison');
    } finally {
      setLoading(false);
    }
  };

  const renderDiff = (val) => {
    if (val > 0) return <span style={{ color: 'var(--danger)' }}>+{val}</span>;
    if (val < 0) return <span style={{ color: 'var(--success)' }}>{val}</span>;
    return <span style={{ color: 'var(--gray-500)' }}>0</span>;
  };

  return (
    <div>
      <div className="page-header"><h1>Comparer deux executions</h1></div>
      <ErrorMessage message={error} />

      <form onSubmit={handleCompare} className="card" style={{ padding: '20px', marginBottom: '16px' }}>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'end' }}>
          <div className="form-group" style={{ marginBottom: 0, flex: 1 }}>
            <label>Execution 1</label>
            <input required value={id1} onChange={e => setId1(e.target.value)} placeholder="PAL-EXEC-000987" />
          </div>
          <div className="form-group" style={{ marginBottom: 0, flex: 1 }}>
            <label>Execution 2</label>
            <input required value={id2} onChange={e => setId2(e.target.value)} placeholder="PAL-EXEC-000988" />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>Comparer</button>
        </div>
      </form>

      {loading && <Loading />}

      {result && (
        <div className="grid-2">
          {[result.execution1, result.execution2].map((exec, idx) => (
            <div key={idx} className="card" style={{ padding: '20px' }}>
              <h3 style={{ marginBottom: '12px' }}>Execution {idx + 1}</h3>
              <p><strong>ID:</strong> {exec.executionId}</p>
              <p><strong>Ruleset:</strong> {exec.rulesetCode}</p>
              <p><strong>Palettes:</strong> {exec.totalPallets}</p>
              <p><strong>Score:</strong> {exec.globalScore}</p>
            </div>
          ))}
          {result.differences && (
            <div className="card" style={{ padding: '20px', gridColumn: '1 / -1' }}>
              <h3 style={{ marginBottom: '12px' }}>Differences</h3>
              <div className="grid-2">
                <p>Difference palettes: {renderDiff(result.differences.palletCountDiff)}</p>
                <p>Difference score: {renderDiff(result.differences.scoreDiff)}</p>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default PalletizationComparePage;
