// TICKET-ADV125 — shared RTL render helper: wraps ui in MemoryRouter,
// ThemeProvider and an authenticated AuthContext.Provider so components
// using useNavigate()/useTheme()/useAuth() (and the withAuth HOC) render
// instead of crashing or redirecting to /login.
import React from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@context/ThemeContext.jsx';
import { AuthContext } from '@context/AuthContext.jsx';

const defaultAuthValue = {
  user: { token: 'test-token', role: 'ADMIN' },
  login: () => {},
  logout: () => {},
};

export function renderWithProviders(ui, { authValue = defaultAuthValue, route = '/' } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider>
        <AuthContext.Provider value={authValue}>
          {ui}
        </AuthContext.Provider>
      </ThemeProvider>
    </MemoryRouter>
  );
}
