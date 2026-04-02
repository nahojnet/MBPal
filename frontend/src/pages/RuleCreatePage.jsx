import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ruleApi } from '../services/api';
import ErrorMessage from '../components/common/ErrorMessage';
import { RULE_SCOPES, RULE_SEVERITIES, OPERATORS, CONDITION_FIELDS, EFFECT_TYPES } from '../utils/constants';

const STEPS = ['Informations', 'Conditions', 'Effet', 'Explication', 'Resume'];

function RuleCreatePage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    ruleCode: '',
    description: '',
    scope: 'PACKAGE',
    severity: 'HARD',
    weight: 50,
    conditions: [{ subject: '', field: '', operator: '=', value: '' }],
    combinator: 'all',
    effectType: 'SET_ATTRIBUTE',
    effectParams: {},
    explanation: '',
  });

  const fields = CONDITION_FIELDS[form.scope] || CONDITION_FIELDS.PACKAGE;

  const addCondition = () => setForm(f => ({ ...f, conditions: [...f.conditions, { subject: '', field: '', operator: '=', value: '' }] }));
  const removeCondition = (i) => setForm(f => ({ ...f, conditions: f.conditions.filter((_, idx) => idx !== i) }));
  const updateCondition = (i, key, val) => {
    setForm(f => ({ ...f, conditions: f.conditions.map((c, idx) => idx === i ? { ...c, [key]: val } : c) }));
  };

  const buildConditionJson = () => {
    const conditions = form.conditions.filter(c => c.field).map(c => {
      const cond = { field: c.field, operator: c.operator, value: isNaN(c.value) ? c.value : Number(c.value) };
      if (form.scope === 'INTER_PACKAGE' && c.subject) cond.subject = c.subject;
      return cond;
    });
    return { [form.combinator]: conditions };
  };

  const buildEffectJson = () => {
    const effect = { type: form.effectType, ...form.effectParams };
    return effect;
  };

  const handleSubmit = async () => {
    setError('');
    try {
      await ruleApi.create({
        ruleCode: form.ruleCode,
        scope: form.scope,
        severity: form.severity,
        description: form.description,
        version: {
          conditionJson: buildConditionJson(),
          effectJson: buildEffectJson(),
          explanation: form.explanation,
        },
      });
      navigate('/rules');
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors de la creation');
    }
  };

  const renderEffectParams = () => {
    const t = form.effectType;
    const set = (k, v) => setForm(f => ({ ...f, effectParams: { ...f.effectParams, [k]: v } }));

    if (t === 'SET_ATTRIBUTE') return (
      <div className="grid-2">
        <div className="form-group">
          <label>Attribut</label>
          <select value={form.effectParams.attribute || ''} onChange={e => set('attribute', e.target.value)}>
            <option value="">-- Choisir --</option>
            <option value="stackingClass">Classe empilage</option>
            <option value="temperatureGroup">Groupe temperature</option>
          </select>
        </div>
        <div className="form-group">
          <label>Valeur</label>
          <input value={form.effectParams.value || ''} onChange={e => set('value', e.target.value)} placeholder="BOTTOM, TOP, MIDDLE..." />
        </div>
      </div>
    );

    if (t === 'GROUP_BY') return (
      <div className="form-group">
        <label>Attribut de regroupement</label>
        <select value={form.effectParams.attribute || ''} onChange={e => set('attribute', e.target.value)}>
          <option value="">-- Choisir --</option>
          <option value="temperature_type">Temperature</option>
          <option value="fragility_level">Fragilite</option>
          <option value="product_code">Code produit</option>
        </select>
      </div>
    );

    if (t === 'SET_CONSTRAINT') return (
      <div className="grid-3">
        <div className="form-group">
          <label>Contrainte</label>
          <select value={form.effectParams.constraint || ''} onChange={e => set('constraint', e.target.value)}>
            <option value="">-- Choisir --</option>
            <option value="maxHeight">Hauteur max</option>
            <option value="maxWeight">Poids max</option>
            <option value="maxVolume">Volume max</option>
            <option value="maxBoxCount">Nb colis max</option>
          </select>
        </div>
        <div className="form-group">
          <label>Valeur</label>
          <input type="number" value={form.effectParams.value || ''} onChange={e => set('value', Number(e.target.value))} />
        </div>
        <div className="form-group">
          <label>Unite</label>
          <select value={form.effectParams.unit || ''} onChange={e => set('unit', e.target.value)}>
            <option value="mm">mm</option>
            <option value="kg">kg</option>
            <option value="cm3">cm3</option>
          </select>
        </div>
      </div>
    );

    if (t === 'FORBID_ABOVE') return (
      <div className="grid-2">
        <div className="form-group">
          <label>Au-dessus</label>
          <select value={form.effectParams.above || ''} onChange={e => set('above', e.target.value)}>
            <option value="packageA">Colis A</option>
            <option value="packageB">Colis B</option>
          </select>
        </div>
        <div className="form-group">
          <label>En-dessous</label>
          <select value={form.effectParams.below || ''} onChange={e => set('below', e.target.value)}>
            <option value="packageA">Colis A</option>
            <option value="packageB">Colis B</option>
          </select>
        </div>
      </div>
    );

    if (['PREFERRED_SUPPORT', 'REQUIRED_SUPPORT'].includes(t)) return (
      <div className="form-group">
        <label>Support</label>
        <input value={form.effectParams.support || ''} onChange={e => set('support', e.target.value)} placeholder="EURO, HALF, DOLLY" />
      </div>
    );

    if (t === 'MINIMIZE_MIX') return (
      <div className="form-group">
        <label>Attribut</label>
        <input value={form.effectParams.attribute || ''} onChange={e => set('attribute', e.target.value)} placeholder="temperature_type" />
      </div>
    );

    if (t === 'STACKING_PRIORITY') return (
      <div className="form-group">
        <label>Priorite (1=bas, 10=haut)</label>
        <input type="number" min="1" max="10" value={form.effectParams.priority || ''} onChange={e => set('priority', Number(e.target.value))} />
      </div>
    );

    return <p style={{ color: 'var(--gray-500)' }}>Aucun parametre supplementaire requis.</p>;
  };

  return (
    <div>
      <div className="page-header"><h1>Creer une regle</h1></div>
      <ErrorMessage message={error} />

      <div style={{ display: 'flex', gap: '8px', marginBottom: '24px' }}>
        {STEPS.map((s, i) => (
          <div key={s} style={{
            padding: '8px 16px',
            borderRadius: 'var(--radius)',
            background: i === step ? 'var(--primary)' : i < step ? 'var(--success-light)' : 'var(--gray-100)',
            color: i === step ? 'white' : i < step ? 'var(--success)' : 'var(--gray-500)',
            fontWeight: i === step ? '600' : '400',
            cursor: 'pointer',
          }} onClick={() => i < step && setStep(i)}>
            {i + 1}. {s}
          </div>
        ))}
      </div>

      <div className="card" style={{ padding: '24px' }}>
        {step === 0 && (
          <>
            <h3 style={{ marginBottom: '16px' }}>Informations de base</h3>
            <div className="form-group">
              <label>Code</label>
              <input value={form.ruleCode} onChange={e => setForm(f => ({ ...f, ruleCode: e.target.value }))} placeholder="R_MY_RULE" />
            </div>
            <div className="form-group">
              <label>Description</label>
              <textarea rows="2" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label>Portee</label>
                <select value={form.scope} onChange={e => setForm(f => ({ ...f, scope: e.target.value }))}>
                  {RULE_SCOPES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label>Severite</label>
                <select value={form.severity} onChange={e => setForm(f => ({ ...f, severity: e.target.value }))}>
                  {RULE_SEVERITIES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              </div>
            </div>
            {form.severity === 'SOFT' && (
              <div className="form-group">
                <label>Poids (0-100)</label>
                <input type="number" min="0" max="100" value={form.weight} onChange={e => setForm(f => ({ ...f, weight: Number(e.target.value) }))} />
              </div>
            )}
          </>
        )}

        {step === 1 && (
          <>
            <h3 style={{ marginBottom: '16px' }}>Conditions</h3>
            <div className="form-group">
              <label>Combinateur</label>
              <select value={form.combinator} onChange={e => setForm(f => ({ ...f, combinator: e.target.value }))}>
                <option value="all">TOUTES les conditions (ET)</option>
                <option value="any">AU MOINS UNE condition (OU)</option>
              </select>
            </div>
            {form.conditions.map((cond, i) => (
              <div key={i} className="card" style={{ padding: '12px', marginBottom: '8px', background: 'var(--gray-50)' }}>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'end' }}>
                  {form.scope === 'INTER_PACKAGE' && (
                    <div className="form-group" style={{ marginBottom: 0, minWidth: '100px' }}>
                      <label>Sujet</label>
                      <select value={cond.subject} onChange={e => updateCondition(i, 'subject', e.target.value)}>
                        <option value="">--</option>
                        <option value="packageA">Colis A</option>
                        <option value="packageB">Colis B</option>
                      </select>
                    </div>
                  )}
                  <div className="form-group" style={{ marginBottom: 0, flex: 1 }}>
                    <label>Champ</label>
                    <select value={cond.field} onChange={e => updateCondition(i, 'field', e.target.value)}>
                      <option value="">-- Choisir --</option>
                      {fields.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
                    </select>
                  </div>
                  <div className="form-group" style={{ marginBottom: 0, minWidth: '80px' }}>
                    <label>Operateur</label>
                    <select value={cond.operator} onChange={e => updateCondition(i, 'operator', e.target.value)}>
                      {OPERATORS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                  </div>
                  <div className="form-group" style={{ marginBottom: 0, flex: 1 }}>
                    <label>Valeur</label>
                    {(() => {
                      const fieldDef = fields.find(f => f.value === cond.field);
                      if (fieldDef?.type === 'enum') {
                        return (
                          <select value={cond.value} onChange={e => updateCondition(i, 'value', e.target.value)}>
                            <option value="">-- Choisir --</option>
                            {fieldDef.options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                          </select>
                        );
                      }
                      return <input value={cond.value} onChange={e => updateCondition(i, 'value', e.target.value)} />;
                    })()}
                  </div>
                  {form.conditions.length > 1 && (
                    <button type="button" className="btn btn-outline" onClick={() => removeCondition(i)} style={{ padding: '8px' }}>X</button>
                  )}
                </div>
              </div>
            ))}
            <button type="button" className="btn btn-outline" onClick={addCondition}>+ Ajouter condition</button>
            <div style={{ marginTop: '16px', padding: '12px', background: 'var(--gray-100)', borderRadius: 'var(--radius)', fontSize: '12px', fontFamily: 'monospace' }}>
              {JSON.stringify(buildConditionJson(), null, 2)}
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <h3 style={{ marginBottom: '16px' }}>Effet</h3>
            <div className="form-group">
              <label>Type d'effet</label>
              <select value={form.effectType} onChange={e => setForm(f => ({ ...f, effectType: e.target.value, effectParams: {} }))}>
                {EFFECT_TYPES.map(et => <option key={et.value} value={et.value}>{et.category} - {et.label}</option>)}
              </select>
            </div>
            {renderEffectParams()}
            <div style={{ marginTop: '16px', padding: '12px', background: 'var(--gray-100)', borderRadius: 'var(--radius)', fontSize: '12px', fontFamily: 'monospace' }}>
              {JSON.stringify(buildEffectJson(), null, 2)}
            </div>
          </>
        )}

        {step === 3 && (
          <>
            <h3 style={{ marginBottom: '16px' }}>Explication</h3>
            <div className="form-group">
              <label>Explication metier (obligatoire)</label>
              <textarea rows="4" value={form.explanation} onChange={e => setForm(f => ({ ...f, explanation: e.target.value }))} placeholder="Decrivez pourquoi cette regle existe et ce qu'elle fait..." />
            </div>
          </>
        )}

        {step === 4 && (
          <>
            <h3 style={{ marginBottom: '16px' }}>Resume</h3>
            <table>
              <tbody>
                <tr><td style={{ fontWeight: '600' }}>Code</td><td>{form.ruleCode}</td></tr>
                <tr><td style={{ fontWeight: '600' }}>Portee</td><td>{form.scope}</td></tr>
                <tr><td style={{ fontWeight: '600' }}>Severite</td><td>{form.severity}{form.severity === 'SOFT' ? ` (poids: ${form.weight})` : ''}</td></tr>
                <tr><td style={{ fontWeight: '600' }}>Description</td><td>{form.description}</td></tr>
                <tr><td style={{ fontWeight: '600' }}>Conditions</td><td><code>{JSON.stringify(buildConditionJson())}</code></td></tr>
                <tr><td style={{ fontWeight: '600' }}>Effet</td><td><code>{JSON.stringify(buildEffectJson())}</code></td></tr>
                <tr><td style={{ fontWeight: '600' }}>Explication</td><td>{form.explanation}</td></tr>
              </tbody>
            </table>
          </>
        )}

        <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'space-between' }}>
          {step > 0 ? <button className="btn btn-outline" onClick={() => setStep(s => s - 1)}>Precedent</button> : <div />}
          {step < STEPS.length - 1 ? (
            <button className="btn btn-primary" onClick={() => setStep(s => s + 1)}>Suivant</button>
          ) : (
            <button className="btn btn-success" onClick={handleSubmit}>Sauvegarder (brouillon)</button>
          )}
        </div>
      </div>
    </div>
  );
}

export default RuleCreatePage;
