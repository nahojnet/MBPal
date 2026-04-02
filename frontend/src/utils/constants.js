export const TEMPERATURE_TYPES = [
  { value: 'AMBIANT', label: 'Ambiant', color: '#F59E0B' },
  { value: 'FRAIS', label: 'Frais', color: '#10B981' },
  { value: 'SURGELE', label: 'Surgelé', color: '#3B82F6' },
];

export const FRAGILITY_LEVELS = [
  { value: 'ROBUSTE', label: 'Robuste' },
  { value: 'FRAGILE', label: 'Fragile' },
];

export const RULE_SCOPES = [
  { value: 'PACKAGE', label: 'Colis' },
  { value: 'PALLET', label: 'Palette' },
  { value: 'INTER_PACKAGE', label: 'Inter-colis' },
];

export const RULE_SEVERITIES = [
  { value: 'HARD', label: 'Obligatoire', color: '#EF4444' },
  { value: 'SOFT', label: 'Préférentielle', color: '#F59E0B' },
];

export const OPERATORS = [
  { value: '=', label: '=' },
  { value: '!=', label: '≠' },
  { value: '>', label: '>' },
  { value: '>=', label: '≥' },
  { value: '<', label: '<' },
  { value: '<=', label: '≤' },
  { value: 'IN', label: 'Dans' },
  { value: 'NOT_IN', label: 'Pas dans' },
  { value: 'BETWEEN', label: 'Entre' },
];

export const CONDITION_FIELDS = {
  PACKAGE: [
    { value: 'weight_kg', label: 'Poids (kg)', type: 'number' },
    { value: 'height_mm', label: 'Hauteur (mm)', type: 'number' },
    { value: 'length_mm', label: 'Longueur (mm)', type: 'number' },
    { value: 'width_mm', label: 'Largeur (mm)', type: 'number' },
    { value: 'temperature_type', label: 'Température', type: 'enum', options: TEMPERATURE_TYPES },
    { value: 'fragility_level', label: 'Fragilité', type: 'enum', options: FRAGILITY_LEVELS },
    { value: 'stackable_flag', label: 'Empilable', type: 'boolean' },
    { value: 'product_code', label: 'Code produit', type: 'text' },
  ],
  PALLET: [
    { value: 'support.code', label: 'Type de support', type: 'text' },
    { value: 'totalWeight', label: 'Poids total (kg)', type: 'number' },
    { value: 'totalHeight', label: 'Hauteur totale (mm)', type: 'number' },
    { value: 'boxCount', label: 'Nombre de colis', type: 'number' },
  ],
  INTER_PACKAGE: [
    { value: 'weight_kg', label: 'Poids (kg)', type: 'number' },
    { value: 'height_mm', label: 'Hauteur (mm)', type: 'number' },
    { value: 'temperature_type', label: 'Température', type: 'enum', options: TEMPERATURE_TYPES },
    { value: 'fragility_level', label: 'Fragilité', type: 'enum', options: FRAGILITY_LEVELS },
    { value: 'stackable_flag', label: 'Empilable', type: 'boolean' },
  ],
};

export const EFFECT_TYPES = [
  { value: 'SET_ATTRIBUTE', label: 'Définir un attribut', category: 'Classification' },
  { value: 'GROUP_BY', label: 'Regrouper par attribut', category: 'Compatibilité' },
  { value: 'MUST_BE_TOGETHER', label: 'Doit être ensemble', category: 'Compatibilité' },
  { value: 'MUST_NOT_BE_TOGETHER', label: 'Ne doit pas être ensemble', category: 'Compatibilité' },
  { value: 'MUST_BE_AT_BOTTOM', label: 'Doit être en bas', category: 'Positionnement' },
  { value: 'MUST_BE_AT_TOP', label: 'Doit être en haut', category: 'Positionnement' },
  { value: 'FORBID_ABOVE', label: 'Interdire au-dessus', category: 'Positionnement' },
  { value: 'STACKING_PRIORITY', label: 'Priorité d\'empilage', category: 'Positionnement' },
  { value: 'SET_CONSTRAINT', label: 'Définir une contrainte', category: 'Capacité' },
  { value: 'ALLOWED_SUPPORTS', label: 'Supports autorisés', category: 'Support' },
  { value: 'REQUIRED_SUPPORT', label: 'Support obligatoire', category: 'Support' },
  { value: 'PREFERRED_SUPPORT', label: 'Support préféré', category: 'Support' },
  { value: 'MINIMIZE_PALLETS', label: 'Minimiser les palettes', category: 'Optimisation' },
  { value: 'MINIMIZE_VOID', label: 'Minimiser le vide', category: 'Optimisation' },
  { value: 'MAXIMIZE_STABILITY', label: 'Maximiser la stabilité', category: 'Optimisation' },
  { value: 'MINIMIZE_MIX', label: 'Minimiser les mélanges', category: 'Optimisation' },
];

export const STATUS_COLORS = {
  PENDING: { bg: '#F3F4F6', text: '#6B7280', label: 'En attente' },
  PROCESSING: { bg: '#DBEAFE', text: '#2563EB', label: 'En cours' },
  COMPLETED: { bg: '#D1FAE5', text: '#059669', label: 'Terminé' },
  ERROR: { bg: '#FEE2E2', text: '#DC2626', label: 'Erreur' },
  DRAFT: { bg: '#F3F4F6', text: '#6B7280', label: 'Brouillon' },
  ACTIVE: { bg: '#D1FAE5', text: '#059669', label: 'Actif' },
  ARCHIVED: { bg: '#FEF3C7', text: '#D97706', label: 'Archivé' },
};

export const TEMPERATURE_COLORS = {
  AMBIANT: '#F59E0B',
  FRAIS: '#10B981',
  SURGELE: '#3B82F6',
};
