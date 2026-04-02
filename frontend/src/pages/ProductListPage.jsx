import { useState, useEffect } from 'react';
import { productApi } from '../services/api';
import TemperatureBadge from '../components/common/TemperatureBadge';
import Loading from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';
import { TEMPERATURE_TYPES, FRAGILITY_LEVELS } from '../utils/constants';

function ProductListPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    productCode: '', label: '', temperatureType: 'AMBIANT',
    lengthMm: '', widthMm: '', heightMm: '', weightKg: '',
    fragilityLevel: 'ROBUSTE', stackableFlag: true,
  });

  const loadData = () => {
    setLoading(true);
    productApi.list({ page: 0, size: 100 })
      .then(res => setProducts(res.data.content || res.data || []))
      .catch(() => setProducts([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await productApi.create({
        ...form,
        lengthMm: Number(form.lengthMm),
        widthMm: Number(form.widthMm),
        heightMm: Number(form.heightMm),
        weightKg: Number(form.weightKg),
      });
      setShowForm(false);
      setForm({ productCode: '', label: '', temperatureType: 'AMBIANT', lengthMm: '', widthMm: '', heightMm: '', weightKg: '', fragilityLevel: 'ROBUSTE', stackableFlag: true });
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur');
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>Produits</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Annuler' : 'Ajouter produit'}</button>
      </div>

      <ErrorMessage message={error} />

      {showForm && (
        <form onSubmit={handleSubmit} className="card" style={{ padding: '20px', marginBottom: '16px' }}>
          <div className="grid-3">
            <div className="form-group"><label>Code</label><input required value={form.productCode} onChange={e => setForm(f => ({ ...f, productCode: e.target.value }))} /></div>
            <div className="form-group"><label>Label</label><input required value={form.label} onChange={e => setForm(f => ({ ...f, label: e.target.value }))} /></div>
            <div className="form-group"><label>Temperature</label>
              <select value={form.temperatureType} onChange={e => setForm(f => ({ ...f, temperatureType: e.target.value }))}>
                {TEMPERATURE_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div className="form-group"><label>Longueur (mm)</label><input type="number" required value={form.lengthMm} onChange={e => setForm(f => ({ ...f, lengthMm: e.target.value }))} /></div>
            <div className="form-group"><label>Largeur (mm)</label><input type="number" required value={form.widthMm} onChange={e => setForm(f => ({ ...f, widthMm: e.target.value }))} /></div>
            <div className="form-group"><label>Hauteur (mm)</label><input type="number" required value={form.heightMm} onChange={e => setForm(f => ({ ...f, heightMm: e.target.value }))} /></div>
            <div className="form-group"><label>Poids (kg)</label><input type="number" step="0.01" required value={form.weightKg} onChange={e => setForm(f => ({ ...f, weightKg: e.target.value }))} /></div>
            <div className="form-group"><label>Fragilite</label>
              <select value={form.fragilityLevel} onChange={e => setForm(f => ({ ...f, fragilityLevel: e.target.value }))}>
                {FRAGILITY_LEVELS.map(fl => <option key={fl.value} value={fl.value}>{fl.label}</option>)}
              </select>
            </div>
            <div className="form-group"><label>Empilable</label>
              <select value={form.stackableFlag ? 'true' : 'false'} onChange={e => setForm(f => ({ ...f, stackableFlag: e.target.value === 'true' }))}>
                <option value="true">Oui</option>
                <option value="false">Non</option>
              </select>
            </div>
          </div>
          <button type="submit" className="btn btn-success">Enregistrer</button>
        </form>
      )}

      <div className="card">
        {loading ? <Loading /> : (
          <table>
            <thead>
              <tr><th>Code</th><th>Label</th><th>Temperature</th><th>Dimensions</th><th>Poids</th><th>Fragilite</th><th>Empilable</th></tr>
            </thead>
            <tbody>
              {products.length === 0 ? (
                <tr><td colSpan="7" style={{ textAlign: 'center', color: 'var(--gray-500)' }}>Aucun produit</td></tr>
              ) : products.map(p => (
                <tr key={p.productCode}>
                  <td><strong>{p.productCode}</strong></td>
                  <td>{p.label}</td>
                  <td><TemperatureBadge type={p.temperatureType} /></td>
                  <td>{p.lengthMm} x {p.widthMm} x {p.heightMm} mm</td>
                  <td>{p.weightKg} kg</td>
                  <td>{p.fragilityLevel}</td>
                  <td>{p.stackableFlag ? 'Oui' : 'Non'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default ProductListPage;
