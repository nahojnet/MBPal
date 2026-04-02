import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { palletizationApi, supportApi, rulesetApi } from '../services/api';
import ErrorMessage from '../components/common/ErrorMessage';

function PalletizationLaunchPage() {
  const navigate = useNavigate();
  const [supports, setSupports] = useState([]);
  const [rulesets, setRulesets] = useState([]);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    externalOrderId: '',
    customerId: '',
    customerName: '',
    warehouseCode: '',
    rulesetCode: '',
    allowedSupports: [],
    lines: [{ productCode: '', boxQuantity: 1 }],
    dryRun: false,
  });

  useEffect(() => {
    supportApi.list({ active: true }).then(res => {
      const data = res.data.content || res.data || [];
      setSupports(data);
      setForm(f => ({ ...f, allowedSupports: data.map(s => s.supportCode) }));
    }).catch(() => {});
    rulesetApi.list({ status: 'ACTIVE' }).then(res => {
      const data = res.data.content || res.data || [];
      setRulesets(data);
      if (data.length > 0) setForm(f => ({ ...f, rulesetCode: data[0].rulesetCode }));
    }).catch(() => {});
  }, []);

  const addLine = () => setForm(f => ({ ...f, lines: [...f.lines, { productCode: '', boxQuantity: 1 }] }));
  const removeLine = (i) => setForm(f => ({ ...f, lines: f.lines.filter((_, idx) => idx !== i) }));
  const updateLine = (i, field, value) => {
    setForm(f => ({
      ...f,
      lines: f.lines.map((l, idx) => idx === i ? { ...l, [field]: value } : l)
    }));
  };

  const toggleSupport = (code) => {
    setForm(f => ({
      ...f,
      allowedSupports: f.allowedSupports.includes(code)
        ? f.allowedSupports.filter(c => c !== code)
        : [...f.allowedSupports, code]
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const payload = {
        externalOrderId: form.externalOrderId,
        customerId: form.customerId,
        customerName: form.customerName,
        warehouseCode: form.warehouseCode,
        rulesetCode: form.rulesetCode,
        supportPolicy: { allowedSupports: form.allowedSupports },
        lines: form.lines.filter(l => l.productCode).map(l => ({
          productCode: l.productCode,
          boxQuantity: parseInt(l.boxQuantity) || 1,
        })),
      };
      const api = form.dryRun ? palletizationApi.simulate : palletizationApi.submit;
      const res = await api(payload);
      navigate(`/palletization/${res.data.executionId}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors de la soumission');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="page-header"><h1>Nouvelle palettisation</h1></div>
      <ErrorMessage message={error} />
      <form onSubmit={handleSubmit}>
        <div className="card" style={{ padding: '20px', marginBottom: '16px' }}>
          <h3 style={{ marginBottom: '16px' }}>Commande</h3>
          <div className="grid-2">
            <div className="form-group">
              <label>ID Commande</label>
              <input required value={form.externalOrderId} onChange={e => setForm(f => ({ ...f, externalOrderId: e.target.value }))} placeholder="CMD-2026-000123" />
            </div>
            <div className="form-group">
              <label>Client ID</label>
              <input required value={form.customerId} onChange={e => setForm(f => ({ ...f, customerId: e.target.value }))} placeholder="C001" />
            </div>
            <div className="form-group">
              <label>Nom client</label>
              <input value={form.customerName} onChange={e => setForm(f => ({ ...f, customerName: e.target.value }))} />
            </div>
            <div className="form-group">
              <label>Entrepot</label>
              <input value={form.warehouseCode} onChange={e => setForm(f => ({ ...f, warehouseCode: e.target.value }))} />
            </div>
          </div>
        </div>

        <div className="card" style={{ padding: '20px', marginBottom: '16px' }}>
          <h3 style={{ marginBottom: '16px' }}>Supports autorises</h3>
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            {supports.map(s => (
              <label key={s.supportCode} style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
                <input type="checkbox" checked={form.allowedSupports.includes(s.supportCode)} onChange={() => toggleSupport(s.supportCode)} />
                {s.label} ({s.supportCode})
              </label>
            ))}
          </div>
        </div>

        <div className="card" style={{ padding: '20px', marginBottom: '16px' }}>
          <h3 style={{ marginBottom: '16px' }}>Ruleset</h3>
          <div className="form-group">
            <select value={form.rulesetCode} onChange={e => setForm(f => ({ ...f, rulesetCode: e.target.value }))}>
              <option value="">-- Selectionner --</option>
              {rulesets.map(r => (
                <option key={r.rulesetCode} value={r.rulesetCode}>{r.label} ({r.rulesetCode})</option>
              ))}
            </select>
          </div>
          <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
            <input type="checkbox" checked={form.dryRun} onChange={e => setForm(f => ({ ...f, dryRun: e.target.checked }))} />
            Mode simulation (dry run)
          </label>
        </div>

        <div className="card" style={{ padding: '20px', marginBottom: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3>Lignes de commande</h3>
            <button type="button" className="btn btn-outline" onClick={addLine}>+ Ajouter ligne</button>
          </div>
          <table>
            <thead>
              <tr><th>Code produit</th><th>Quantite colis</th><th></th></tr>
            </thead>
            <tbody>
              {form.lines.map((line, i) => (
                <tr key={i}>
                  <td><input required value={line.productCode} onChange={e => updateLine(i, 'productCode', e.target.value)} placeholder="PROD-001" style={{ width: '100%' }} /></td>
                  <td><input type="number" min="1" required value={line.boxQuantity} onChange={e => updateLine(i, 'boxQuantity', e.target.value)} style={{ width: '100px' }} /></td>
                  <td>{form.lines.length > 1 && <button type="button" className="btn btn-outline" onClick={() => removeLine(i)} style={{ padding: '4px 8px' }}>X</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <button type="submit" className="btn btn-primary" disabled={submitting} style={{ fontSize: '16px', padding: '12px 24px' }}>
          {submitting ? 'Envoi...' : form.dryRun ? 'Simuler' : 'Lancer la palettisation'}
        </button>
      </form>
    </div>
  );
}

export default PalletizationLaunchPage;
