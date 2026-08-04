// TICKET-ADV104 / TICKET-ADV105 — EventSource live feed with prepend + slide-in animation.
(function () {
  const FEED_EL = document.getElementById('trade-feed');
  if (!FEED_EL) return;

  // This static page has no login flow of its own, so it can't read a token
  // from sessionStorage the way the React app does. Native EventSource also
  // can't send an Authorization header. Pass a token on this page's own URL
  // instead, e.g. dashboard.html?token=<jwt> (copy one from POST /auth/login).
  const pageToken = new URLSearchParams(window.location.search).get('token');
  const STREAM_URL = 'http://localhost:8080/api/v1/trades/stream'
    + (pageToken ? '?token=' + encodeURIComponent(pageToken) : '');
  const STATUS_EL = document.getElementById('sse-status');
  let sse = null;

  function updateConnectionBadge(text, variant) {
    if (!STATUS_EL) return;
    STATUS_EL.textContent = text;
    STATUS_EL.className = 'sse-status sse-status--' + variant;
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  const numberFmt = new Intl.NumberFormat('en-US');
  const priceFmt = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 });

  function statusModifier(status) {
    const s = String(status || '').toUpperCase();
    if (s === 'CONFIRMED' || s === 'SETTLED') return 'matched';
    if (s === 'CANCELLED') return 'break';
    return 'pending';
  }

  function prependTradeRow(trade) {
    const el = document.createElement('article');
    el.className = 'trade-card trade-card--' + statusModifier(trade.status) + ' trade-card--new';
    el.innerHTML = `
      <header class="trade-card__header">
        <strong>${escapeHtml(trade.tradeRef)}</strong>
        <span> [${escapeHtml(trade.status)}]</span>
      </header>
      <div class="trade-card__body">
        <span>${escapeHtml(trade.instrumentSymbol)}</span>
        <span> qty=${numberFmt.format(trade.quantity)} </span>
        <span> price=${priceFmt.format(trade.price)} </span>
      </div>`;
    FEED_EL.prepend(el);
    setTimeout(() => el.classList.remove('trade-card--new'), 500);
    while (FEED_EL.children.length > 50) FEED_EL.lastElementChild.remove();
  }

  function connect() {
    sse = new EventSource(STREAM_URL);

    sse.onopen = () => updateConnectionBadge('Live', 'live');

    sse.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);
        prependTradeRow(trade);
      } catch (err) {
        console.error('Failed to parse SSE payload', err);
      }
    };

    // Critical: never call connect() again here — EventSource auto-reconnects
    // with its own backoff. Manual reconnects would DDoS the dev server.
    sse.onerror = () => updateConnectionBadge('Reconnecting…', 'reconnecting');
  }

  window.addEventListener('beforeunload', () => sse?.close());

  updateConnectionBadge('Connecting…', 'connecting');
  connect();
})();
