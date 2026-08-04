import React, { useEffect, useState } from 'react';
import { useAuth } from '@context/AuthContext.jsx';

function useClock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);
  return now;
}

export default function PageTopbar({ title, subtitle }) {
  const { user } = useAuth();
  const now = useClock();

  return (
    <header className="page-topbar">
      <div>
        <h1>{title}</h1>
        {subtitle && <div className="subtitle">{subtitle}</div>}
      </div>
      <div className="page-topbar__right">
        <span className="page-topbar__clock">
          {now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
        </span>
        {user && <span className="page-topbar__user">{user.email || user.role}</span>}
      </div>
    </header>
  );
}
