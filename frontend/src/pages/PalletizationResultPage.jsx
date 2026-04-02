import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { palletizationApi } from '../services/api';
import StatusBadge from '../components/common/StatusBadge';
import TemperatureBadge from '../components/common/TemperatureBadge';
import Loading from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';
import { TEMPERATURE_COLORS } from '../utils/constants';

function PalletizationResultPage() {
  const { executionId } = useParams();
  const [result, setResult] = useState(null);
  const [explanations, setExplanations] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showExplanations, setShowExplanations] = useState(false);

  useEffect(() => {
    setLoading(true);
    palletizationApi.get(executionId)
      .then(res => setResult(res.data))
      .catch(err => setError(err.response?.data?.message || 'Execution introuvable'))
      .finally(() => setLoading(false));
  }, [executionId]);

  const loadExplanations = () => {
    if (explanations) { setShowExplanations(!showExplanations); return; }
    palletizationApi.getExplanations(executionId)
      .then(res => { setExplanations(res.data); setShowExplanations(true); })
      .catch(() => {});
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} />;
  if (!result) return null;

  return (
    <div>
      <div className="page-header">
        <h1>{executionId}</h1>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="btn btn-outline" onClick={loadExplanations}>
            {showExplanations ? 'Masquer' : 'Voir'} les explications
          </button>
          <Link to="/palletization/history" className="btn btn-outline">Retour</Link>
        </div>
      </div>

      <div className="card" style={{ padding: '20px', marginBottom: '16px' }}>
        <div className="grid-4">
          <div><span style={{ color: 'var(--gray-500)' }}>Commande</span><br/><strong>{result.externalOrderId}</strong></div>
          <div><span style={{ color: 'var(--gray-500)' }}>Statut</span><br/><StatusBadge status={result.status} /></div>
          <div><span style={{ color: 'var(--gray-500)' }}>Ruleset</span><br/><strong>{result.rulesetCode}</strong></div>
          <div><span style={{ color: 'var(--gray-500)' }}>Duree</span><br/><strong>{result.durationMs != null ? `${(result.durationMs/1000).toFixed(1)}s` : '-'}</strong></div>
        </div>
        <div className="grid-4" style={{ marginTop: '16px' }}>
          <div><span style={{ color: 'var(--gray-500)' }}>Palettes</span><br/><strong style={{ fontSize: '24px' }}>{result.totalPallets ?? '-'}</strong></div>
          <div><span style={{ color: 'var(--gray-500)' }}>Colis</span><br/><strong style={{ fontSize: '24px' }}>{result.totalBoxes ?? '-'}</strong></div>
          <div><span style={{ color: 'var(--gray-500)' }}>Score</span><br/><strong style={{ fontSize: '24px', color: 'var(--primary)' }}>{result.globalScore ?? '-'}</strong></div>
          <div><span style={{ color: 'var(--gray-500)' }}>Violations</span><br/><strong style={{ fontSize: '24px', color: result.violations?.length ? 'var(--danger)' : 'var(--success)' }}>{result.violations?.length ?? 0}</strong></div>
        </div>
      </div>

      {result.errorMessage && (
        <ErrorMessage message={result.errorMessage} />
      )}

      {result.pallets?.map(pallet => (
        <div key={pallet.palletNumber} className="card" style={{ padding: '20px', marginBottom: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3>Palette {pallet.palletNumber} ({pallet.supportType})</h3>
            <div style={{ display: 'flex', gap: '16px', fontSize: '13px', color: 'var(--gray-500)' }}>
              <span>Poids: <strong>{pallet.totalWeightKg} kg</strong></span>
              <span>Hauteur: <strong>{pallet.totalHeightMm} mm</strong></span>
              <span>Remplissage: <strong>{pallet.fillRatePct}%</strong></span>
              <span>Stabilite: <strong>{pallet.stabilityScore}</strong></span>
            </div>
          </div>
          <div style={{ border: '2px solid var(--gray-300)', borderRadius: 'var(--radius)', overflow: 'hidden' }}>
            {pallet.items?.sort((a, b) => b.layerNo - a.layerNo).map((item, idx) => {
              const bgColor = TEMPERATURE_COLORS[item.temperatureType] || '#F3F4F6';
              return (
                <div key={idx} style={{
                  padding: '8px 16px',
                  background: `${bgColor}15`,
                  borderBottom: idx < pallet.items.length - 1 ? '1px solid var(--gray-200)' : 'none',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}>
                  <div>
                    <strong>{item.productCode}</strong>
                    <span style={{ color: 'var(--gray-500)', marginLeft: '8px' }}>#{item.boxInstanceIndex}</span>
                  </div>
                  <div style={{ display: 'flex', gap: '12px', fontSize: '13px' }}>
                    <span>Couche {item.layerNo}</span>
                    {item.stackingClass && <span className="badge" style={{ background: 'var(--gray-100)', color: 'var(--gray-600)' }}>{item.stackingClass}</span>}
                  </div>
                </div>
              );
            })}
            <div style={{ padding: '6px 16px', background: 'var(--gray-200)', textAlign: 'center', fontWeight: '600', fontSize: '12px', color: 'var(--gray-600)' }}>
              {pallet.supportType}
            </div>
          </div>
        </div>
      ))}

      {showExplanations && explanations && (
        <div className="card" style={{ padding: '20px', marginTop: '16px' }}>
          <h3 style={{ marginBottom: '12px' }}>Explications</h3>
          {explanations.appliedRules?.length > 0 && (
            <>
              <h4 style={{ color: 'var(--gray-600)', marginBottom: '8px' }}>Regles appliquees</h4>
              <table style={{ marginBottom: '16px' }}>
                <thead><tr><th>Regle</th><th>Version</th><th>Severite</th><th>Explication</th></tr></thead>
                <tbody>
                  {explanations.appliedRules.map((r, i) => (
                    <tr key={i}><td>{r.ruleCode}</td><td>{r.version}</td><td>{r.severity}</td><td>{r.explanation}</td></tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
          {explanations.decisionTrace?.length > 0 && (
            <>
              <h4 style={{ color: 'var(--gray-600)', marginBottom: '8px' }}>Trace des decisions</h4>
              <table>
                <thead><tr><th>#</th><th>Etape</th><th>Description</th></tr></thead>
                <tbody>
                  {explanations.decisionTrace.map((t, i) => (
                    <tr key={i}><td>{t.traceOrder}</td><td>{t.stepName}</td><td>{t.description}</td></tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default PalletizationResultPage;
