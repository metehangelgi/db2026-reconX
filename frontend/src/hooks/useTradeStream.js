// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useEffect, useState } from 'react';

export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades, setTrades] = useState([]);
  const [isConnected, setConnected] = useState(false);

  useEffect(() => {
    // Native EventSource can't send an Authorization header, so the JWT rides
    // along as a query param instead — the backend's JwtAuthenticationFilter
    // accepts it there only for this one stream endpoint (see its Javadoc).
    const token = sessionStorage.getItem('reconx-token');
    const streamUrl = token
      ? `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`
      : url;

    const es = new EventSource(streamUrl);

    es.onopen = () => setConnected(true);
    es.onmessage = (e) => {
      const trade = JSON.parse(e.data);
      setTrades(prev => [trade, ...prev].slice(0, 200));
    };
    es.onerror = () => setConnected(false);

    return () => es.close();
  }, [url]);

  return { trades, isConnected };
}
