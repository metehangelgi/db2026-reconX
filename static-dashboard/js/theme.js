// TICKET-ADV100 — theme toggle click wiring. The initial theme read/set
// (needed to avoid a FOUC flash) happens in an inline <script> in <head>,
// before the stylesheet loads — see the inline snippet in each HTML page.
// This file only wires the toggle button once the DOM is ready.
(function () {
  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('theme-toggle');
    btn && btn.addEventListener('click', () => {
      const next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
      document.documentElement.dataset.theme = next;
      localStorage.setItem('reconx-theme', next);
    });
  });
})();
