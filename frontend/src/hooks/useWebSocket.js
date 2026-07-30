// TICKET-ADV115 — useWebSocket(url) with auto-reconnect (exp backoff up to 5 tries).
import { useState, useEffect, useRef, useCallback } from 'react';

export function useWebSocket(url, { reconnect = true, maxRetries = 5 } = {}) {
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('connecting');
  const wsRef = useRef(null);
  const retriesRef = useRef(0);
  const reconnectTimeoutRef = useRef(null);

  useEffect(() => {
    let cancelled = false;

    function connect() {
      setStatus('connecting');
      const ws = new WebSocket(url);
      wsRef.current = ws;

      ws.onopen = () => {
        if (cancelled) return;
        retriesRef.current = 0;
        setStatus('open');
      };

      ws.onmessage = (event) => {
        if (cancelled) return;
        try {
          setData(JSON.parse(event.data));
        } catch {
          setData(event.data);
        }
      };

      ws.onerror = () => {
        if (cancelled) return;
        setStatus('error');
      };

      ws.onclose = () => {
        if (cancelled) return;
        setStatus('closed');
        if (reconnect && retriesRef.current < maxRetries) {
          const delay = Math.min(500 * 2 ** retriesRef.current, 30000);
          retriesRef.current += 1;
          reconnectTimeoutRef.current = setTimeout(connect, delay);
        }
      };
    }

    connect();

    return () => {
      cancelled = true;
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      wsRef.current?.close();
    };
  }, [url, reconnect, maxRetries]);

  const send = useCallback((payload) => {
    const ws = wsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(typeof payload === 'string' ? payload : JSON.stringify(payload));
    }
  }, []);

  return { data, status, send };
}
