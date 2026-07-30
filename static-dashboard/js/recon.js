// TICKET-ADV068-070 — trigger a recon run, load its breaks, resolve them.
(function () {
  const form = document.getElementById('recon-form');
  const tbody = document.getElementById('breaks-tbody');
  const jobStatusEl = document.getElementById('recon-job-status');
  if (!form || !tbody) return;

  const BASE = 'http://localhost:8080/api';
  const token = new URLSearchParams(window.location.search).get('token');

  function authHeaders() {
    return token ? { Authorization: 'Bearer ' + token } : {};
  }

  async function request(method, path, body) {
    const res = await fetch(BASE + path, {
      method,
      headers: Object.assign({ 'Content-Type': 'application/json' }, authHeaders()),
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) {
      throw new Error('HTTP ' + res.status + ': ' + (await res.text().catch(() => res.statusText)));
    }
    if (res.status === 204) return null;
    return res.json();
  }

  function renderBreaks(breaks) {
    tbody.innerHTML = breaks.map((b) => `
      <tr data-id="${b.id}">
        <td>${b.tradeId}</td>
        <td>${b.discrepancyType}</td>
        <td>${b.status}</td>
        <td>${b.detectedAt || ''}</td>
        <td>${b.resolutionNote || ''}</td>
        <td>${b.status === 'OPEN' ? '<button class="resolve-btn">Resolve</button>' : ''}</td>
      </tr>`).join('');
  }

  let currentJobId = null;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    jobStatusEl.textContent = 'Running…';
    try {
      const from = document.getElementById('recon-from').value;
      const to = document.getElementById('recon-to').value;
      const job = await request('POST', '/v1/recon/run', { from, to });
      currentJobId = job.jobId;
      jobStatusEl.textContent = 'Job ' + job.jobId + ' — status: ' + job.status;
      const breaks = await request('GET', '/v1/recon/jobs/' + currentJobId + '/results');
      renderBreaks(breaks);
    } catch (err) {
      jobStatusEl.textContent = 'Error: ' + err.message;
    }
  });

  tbody.addEventListener('click', async (e) => {
    if (!e.target.classList.contains('resolve-btn')) return;
    const row = e.target.closest('tr');
    const id = row.dataset.id;
    const note = window.prompt('Resolution note:');
    if (note === null) return;
    try {
      await request('PUT', '/v1/recon/results/' + id + '/resolve', { note });
      if (currentJobId) {
        const breaks = await request('GET', '/v1/recon/jobs/' + currentJobId + '/results');
        renderBreaks(breaks);
      }
    } catch (err) {
      jobStatusEl.textContent = 'Error: ' + err.message;
    }
  });

  if (!token) {
    jobStatusEl.textContent = 'No token in URL — add ?token=<jwt> to authenticate (see comment in recon.html).';
  }
})();
