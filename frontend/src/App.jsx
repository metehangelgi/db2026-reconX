// TICKET-ADV122 — Lazy + Suspense for route-based code splitting
import React, { Suspense, lazy, useState } from 'react';
import { Routes, Route, NavLink, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { withErrorBoundary } from '@components/withErrorBoundary.jsx';
import { useAuth } from '@context/AuthContext.jsx';
import { IconDashboard, IconTrading, IconAddTrade, IconRecon, IconReports, IconLogout, IconMenu } from '@components/Icons.jsx';

const Dashboard = lazy(() => import('@pages/Dashboard.jsx'));
const Trades    = lazy(() => import('@pages/Trades.jsx'));
const AddTrade  = lazy(() => import('@pages/AddTrade.jsx'));
const Login     = lazy(() => import('@pages/Login.jsx'));
const Recon     = lazy(() => import('@pages/Recon.jsx'));
const Audit     = lazy(() => import('@pages/Audit.jsx'));

const NAV_ITEMS = [
  { to: '/',           label: 'Dashboard',     icon: IconDashboard, end: true },
  { to: '/trades',     label: 'Trading',       icon: IconTrading },
  { to: '/trades/new', label: 'Add Trade',     icon: IconAddTrade },
  { to: '/recon',      label: 'Reconciliation',icon: IconRecon },
  { to: '/audit',      label: 'Reports',       icon: IconReports },
];

function App() {
  const [navOpen, setNavOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  function handleLogout() {
    setNavOpen(false);
    logout();
    navigate('/login');
  }

  const routes = (
    <Suspense fallback={<div className="loader">Loading…</div>}>
      <Routes>
        <Route path="/login"      element={<Login />} />
        <Route path="/"           element={<Dashboard />} />
        <Route path="/trades"     element={<Trades />} />
        <Route path="/trades/new" element={<AddTrade />} />
        <Route path="/recon"      element={<Recon />} />
        <Route path="/audit"      element={<Audit />} />
        <Route path="*"           element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );

  // The login screen is a standalone card, not part of the authenticated
  // app shell — no point showing a sidebar full of pages you can't reach yet.
  if (location.pathname === '/login') {
    return <div className="auth-shell">{routes}</div>;
  }

  return (
    <div className="app-shell">
      <button
        className="mobile-toggle"
        aria-label={navOpen ? 'Close navigation' : 'Open navigation'}
        aria-expanded={navOpen}
        onClick={() => setNavOpen((v) => !v)}
      >
        <IconMenu />
      </button>

      {navOpen && <div className="sidebar__scrim" onClick={() => setNavOpen(false)} />}

      <aside className={`sidebar${navOpen ? ' sidebar--open' : ''}`}>
        <div className="sidebar__brand">
          <div className="sidebar__logo" aria-hidden="true">R</div>
          <span className="sidebar__title">ReconX</span>
        </div>

        <nav className="sidebar__nav" aria-label="Main navigation">
          {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={() => setNavOpen(false)}
              className={({ isActive }) => (isActive ? 'active' : undefined)}
            >
              <Icon />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        {user && (
          <div className="sidebar__footer">
            <button className="sidebar__logout" onClick={handleLogout}>
              <IconLogout />
              <span>Logout</span>
            </button>
          </div>
        )}
      </aside>

      <main className="content">{routes}</main>
    </div>
  );
}

export default withErrorBoundary(App);
