import { useState, useEffect } from 'react';
import { supportApi } from '../services/api';
import Loading from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';

function SupportListPage() {
  const [supports, setSupports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    supportCode: '', label: '', lengthMm: '', widthMm: '', heightMm: '',
    maxLoadKg: '', maxTotalHeightMm: '', mergeableFlag: false, mergeTargetCode: '',
  });

  const loadData = () => {
    setLoading(true);
    supportApi.list()
      .then(res => setSupports(res.data.content || res.data || []))
      .catch(() => setSupports([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await supportApi.create({
        ...form,
        lengthMm: Number(form.lengthMm),
        widthMm: Number(form.widthMm),
        heightMm: Number(form.heightMm),
        maxLoadKg: Number(form.maxLoadKg),
        maxTotalHeightMm: Number(form.maxTotalHeightMm),
      });
      setShowForm(false);
      setForm({ supportCode: '', label: '', lengthMm: '', widthMm: '', heightMm: '', maxLoadKg: '', maxTotalHeightMm: '', mergeableFlag: false, mergeTargetCode: '' });
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur');
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>Supports</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Annuler' : 'Ajouter support'}</button>
      </div>

      <ErrorMessage message={error} />

      {showForm && (
        <form onSubmit={handleSubmit} className="card" style={{ padding: '20px', marginBottom: '16px' }}>
          <div className="grid-3">
            <div className="form-group"><label>Code</label><input required value={form.supportCode} onChange={e => setForm(f => ({ ...f, supportCode: e.target.value }))} /></div>
            <div className="form-group"><label>Label</label><input required value={form.label} onChange={e => setForm(f => ({ ...f, label: e.target.value }))} /></div>
            <div className="form-group"><label>Longueur (mm)</label><input type="number" required value={form.lengthMm} onChange={e => setForm(f => ({ ...f, lengthMm: e.target.value }))} /></div>
            <div className="form-group"><label>Largeur (mm)</label><input type="number" required value={form.widthMm} onChange={e => setForm(f => ({ ...f, widthMm: e.target.value }))} /></div>
            <div className="form-group"><label>Hauteur (mm)</label><input type="number" required value={form.heightMm} onChange={e => setForm(f => ({ ...f, heightMm: e.target.value }))} /></div>
            <div className="form-group"><label>Charge max (kg)</label><input type="number" step="0.01" required value={form.maxLoadKg} onChange={e => setForm(f => ({ ...f, maxLoadKg: e.target.value }))} /></div>
            <div className="form-group"><label>Hauteur max totale (mm)</label><input type="number" required value={form.maxTotalHeightMm} onChange={e => setForm(f => ({ ...f, maxTotalHeightMm: e.target.value }))} /></div>
            <div className="form-group"><label>Fusionnable</label>
              <select value={form.mergeableFlag ? 'true' : 'false'} onChange={e => setForm(f => ({ ...f, mergeableFlag: e.target.value === 'true' }))}>
                <option value="false">Non</option>
                <option value="true">Oui</option>
              </select>
            </div>
            {form.mergeableFlag && (
              <div className="form-group"><label>Support cible fusion</label><input value={form.mergeTargetCode} onChange={e => setForm(f => ({ ...f, mergeTargetCode: e.target.value }))} placeholder="EURO" /></div>
            )}
          </div>
          <button type="submit" className="btn btn-success">Enregistrer</button>
        </form>
      )}

      <div className="card">
        {loading ? <Loading /> : (
          <table>
            <thead>
              <tr><th>Code</th><th>Label</th><th>Dimensions</th><th>Charge max</th><th>Hauteur max</th><th>Fusionnable</th></tr>
            </thead>
            <tbody>
              {supports.length === 0 ? (
                <tr><td colSpan="6" style={{ textAlign: 'center', color: 'var(--gray-500)' }}>Aucun support</td></tr>
              ) : supports.map(s => (
                <tr key={s.supportCode}>
                  <td><strong>{s.supportCode}</strong></td>
                  <td>{s.label}</td>
                  <td>{s.lengthMm} x {s.widthMm} x {s.heightMm} mm</td>
                  <td>{s.maxLoadKg} kg</td>
                  <td>{s.maxTotalHeightMm} mm</td>
                  <td>{s.mergeableFlag ? `Oui → ${s.mergeTargetCode}` : 'Non'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default SupportListPage;
