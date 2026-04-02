import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

// Products
export const productApi = {
  list: (params) => api.get('/products', { params }),
  get: (code) => api.get(`/products/${code}`),
  create: (data) => api.post('/products', data),
  update: (code, data) => api.put(`/products/${code}`, data),
};

// Supports
export const supportApi = {
  list: (params) => api.get('/supports', { params }),
  get: (code) => api.get(`/supports/${code}`),
  create: (data) => api.post('/supports', data),
  update: (code, data) => api.put(`/supports/${code}`, data),
};

// Rules
export const ruleApi = {
  list: (params) => api.get('/rules', { params }),
  get: (code) => api.get(`/rules/${code}`),
  create: (data) => api.post('/rules', data),
  publishVersion: (code, versionId) => api.post(`/rules/${code}/versions/${versionId}/publish`),
  validate: (data) => api.post('/rules/validate', data),
};

// Rulesets
export const rulesetApi = {
  list: (params) => api.get('/rulesets', { params }),
  get: (code) => api.get(`/rulesets/${code}`),
  create: (data) => api.post('/rulesets', data),
  publish: (code) => api.post(`/rulesets/${code}/publish`),
  getPriorities: (code) => api.get(`/rulesets/${code}/priorities`),
  updatePriorities: (code, data) => api.put(`/rulesets/${code}/priorities`, data),
};

// Palletization
export const palletizationApi = {
  submit: (data) => api.post('/palletizations', data),
  get: (executionId) => api.get(`/palletizations/${executionId}`),
  getExplanations: (executionId) => api.get(`/palletizations/${executionId}/explanations`),
  list: (params) => api.get('/palletizations', { params }),
  compare: (data) => api.post('/palletizations/compare', data),
  simulate: (data) => api.post('/palletizations/simulate', data),
};

export default api;
