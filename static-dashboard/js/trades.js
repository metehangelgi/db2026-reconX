// TICKET-ADV106 — sortable / resizable / frozen-header trades table.
(function () {
  const tbody = document.getElementById('trades-tbody');
  const table = document.getElementById('trades-table');
  if (!tbody || !table) return;

  // Demo data — no backend required for the static page (mirrors the demo
  // pattern already used in js/sse.js for the live feed). A real page would
  // fetch('/api/v1/trades?size=200'), but the static dashboard is served by
  // a plain `python3 -m http.server` with no build step / dev-server proxy,
  // so a live fetch would just fail with a CORS/404 error when the backend
  // isn't running on the same origin.
  let rows = [
    { tradeRef: 'EQU-20260603-0001', symbol: 'SAP.DE',  quantity: 1000,    price: 125.50, status: 'MATCHED' },
    { tradeRef: 'FX-20260603-0001',  symbol: 'EUR/USD', quantity: 1000000, price: 1.0852, status: 'PENDING' },
    { tradeRef: 'EQU-20260603-0002', symbol: 'AAPL',    quantity: 500,     price: 178.20, status: 'BREAK' },
    { tradeRef: 'BND-20260603-0001', symbol: 'US10Y',   quantity: 200000,  price: 98.75,  status: 'MATCHED' },
    { tradeRef: 'EQU-20260603-0003', symbol: 'MSFT',    quantity: 750,     price: 412.30, status: 'MATCHED' },
  ];

  function renderRows() {
    tbody.innerHTML = rows.map(r => `
      <tr>
        <td>${r.tradeRef}</td>
        <td>${r.symbol}</td>
        <td>${r.quantity}</td>
        <td>${r.price}</td>
        <td>${r.status}</td>
      </tr>`).join('');
  }

  function sortRows(col, type, dir) {
    const mult = dir === 'ascending' ? 1 : -1;
    rows.sort((a, b) => {
      if (type === 'number') return (a[col] - b[col]) * mult;
      return String(a[col]).localeCompare(String(b[col])) * mult;
    });
  }

  table.querySelectorAll('thead th').forEach(th => {
    th.addEventListener('click', (e) => {
      if (e.target.classList.contains('resize-handle')) return;
      const col = th.dataset.col;
      const type = th.dataset.type;
      const wasAscending = th.getAttribute('aria-sort') === 'ascending';
      const nextDir = wasAscending ? 'descending' : 'ascending';

      table.querySelectorAll('thead th').forEach(other => other.removeAttribute('aria-sort'));
      th.setAttribute('aria-sort', nextDir);

      sortRows(col, type, nextDir);
      renderRows();
    });

    const handle = th.querySelector('.resize-handle');
    if (handle) {
      handle.addEventListener('mousedown', (e) => {
        e.stopPropagation();
        const startX = e.clientX;
        const startWidth = th.offsetWidth;

        function onMouseMove(ev) {
          th.style.width = Math.max(40, startWidth + ev.clientX - startX) + 'px';
        }
        function onMouseUp() {
          document.removeEventListener('mousemove', onMouseMove);
          document.removeEventListener('mouseup', onMouseUp);
        }
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
      });
    }
  });

  renderRows();
})();
