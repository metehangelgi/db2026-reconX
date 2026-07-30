// TICKET-ADV112-related — fetch wrapper that attaches Bearer JWT from sessionStorage.
const BASE = '/api';

function authHeaders() {
  const token = sessionStorage.getItem('reconx-token');
  if (token) {
    return { Authorization: `Bearer ${token}` };
  }
  return {};
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json', ...authHeaders() };
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const detail = await res.text().catch(() => res.statusText);
    throw new Error(`HTTP ${res.status}: ${detail}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  login: (email, password) => request('POST', '/auth/login', { email, password }),
  listTrades: (params = '') => request('GET', `/v1/trades${params}`),
  createTrade: (req) => request('POST', '/v1/trades', req),
  updateTrade: (id, req) => request('PUT', `/v1/trades/${id}`, req),
  updateStatus: (id, status) => request('PATCH', `/v1/trades/${id}/status`, { status }),
  deleteTrade: (id) => request('DELETE', `/v1/trades/${id}`),
  runRecon: (req) => request('POST', '/v1/recon/run', req),
  reconJob: (jobId) => request('GET', `/v1/recon/jobs/${jobId}`),
  reconResults: (jobId) => request('GET', `/v1/recon/jobs/${jobId}/results`),
  resolveBreak: (id, note) => request('PUT', `/v1/recon/results/${id}/resolve`, { note }),
  audit: (tradeRef) => request('GET', `/v1/audit/trades/${tradeRef}`),
  health: () => request('GET', '/actuator/health'),
};
