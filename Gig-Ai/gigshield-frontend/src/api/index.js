import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const authAPI = {
  signup: (data) => api.post('/auth/signup', data),
  login: (data) => api.post('/auth/login', data),
};

export const userAPI = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data) => api.put('/user/profile', data),
  topUpWallet: (amount) => api.post('/user/wallet/topup', { amount }),
  withdraw: (amount) => api.post('/user/wallet/withdraw', { amount }),
};

export const dashboardAPI = {
  get: () => api.get('/dashboard'),
};

export const policyAPI = {
  getPlans: () => api.get('/policy/plans'),
  activate: (plan) => api.post('/policy/activate', { plan }),
  payAndActivate: (plan, paymentMethod) => api.post('/policy/pay-and-activate', { plan, paymentMethod }),
  getActive: () => api.get('/policy'),
};

export const claimAPI = {
  getAll: () => api.get('/claims'),
  trigger: (reason) => api.post('/claims/trigger', { reason }),
};

export const riskAPI = {
  get: () => api.get('/risk'),
};

export const transactionAPI = {
  getAll: () => api.get('/transactions'),
};

export const fraudAPI = {
  recordActivity: (data) => api.post('/fraud/activity', data),
  myAlerts: () => api.get('/fraud/alerts/me'),
  allAlerts: () => api.get('/fraud/alerts'),
  resolveAlert: (id) => api.put(`/fraud/alerts/${id}/resolve`),
  stats: () => api.get('/fraud/stats'),
};

export const mlAPI = {
  status: () => api.get('/ml/status'),
  analyze: () => api.get('/ml/analyze'),
  predictRisk: (data) => api.post('/ml/predict/risk', data),
  predictFraud: (data) => api.post('/ml/predict/fraud', data),
  predictPayout: (data) => api.post('/ml/predict/payout', data),
};

export const adminAPI = {
  getUsers: () => api.get('/admin/users'),
  getClaims: () => api.get('/admin/claims'),
  getStats: () => api.get('/admin/stats'),
};

export default api;
