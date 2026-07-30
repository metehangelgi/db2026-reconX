// TICKET-ADV117 — useDebouncedSearch(query, delay).
import { useState, useEffect } from 'react';

export function useDebouncedSearch(query, delay = 300) {
  const [debounced, setDebounced] = useState(query);

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(query), delay);
    return () => clearTimeout(handle);
  }, [query, delay]);

  return debounced;
}
