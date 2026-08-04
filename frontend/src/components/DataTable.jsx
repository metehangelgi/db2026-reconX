// TICKET-ADV114 — Compound <DataTable> with Header / Body / Pagination subcomponents.
import React, { createContext, useContext } from 'react';

const DataTableContext = createContext({ sort: null, page: 0, size: 20, columnCount: undefined });

export default function DataTable({ children, sort, page = 0, size = 20, onSortChange, columnCount }) {
  return (
    <DataTableContext.Provider value={{ sort, page, size, onSortChange, columnCount }}>
      <div className="data-table">{children}</div>
    </DataTableContext.Provider>
  );
}

// TICKET-ADV121 — memoised so that passing stable (useCallback'd / already-stable)
// handler props from parents actually skips re-renders. Without React.memo here,
// wrapping handlers upstream in useCallback would have no effect.
DataTable.Header = React.memo(function Header({ columns }) {
  const { sort, onSortChange } = useContext(DataTableContext);
  return (
    <div className="data-table__header" role="row" style={{ gridTemplateColumns: `repeat(${columns.length}, 1fr)` }}>
      {columns.map((c) => (
        <button
          key={c.key}
          onClick={() => onSortChange?.(c.key)}
          className={sort === c.key ? 'data-table__col--active' : undefined}
        >
          {c.label}
        </button>
      ))}
    </div>
  );
});

DataTable.Body = React.memo(function Body({ rows, render }) {
  const { columnCount } = useContext(DataTableContext);
  const rowStyle = columnCount ? { gridTemplateColumns: `repeat(${columnCount}, 1fr)` } : undefined;
  return (
    <div className="data-table__body">
      {rows.map((row, i) => (
        <div className="data-table__row" key={row.id ?? i} style={rowStyle}>
          {render(row)}
        </div>
      ))}
    </div>
  );
});

DataTable.Pagination = React.memo(function Pagination({ page, totalPages, onChange }) {
  return (
    <nav className="data-table__pagination" aria-label="Pagination">
      <button disabled={page === 0} onClick={() => onChange(page - 1)}>
        ‹
      </button>
      <span>
        {page + 1} / {totalPages}
      </span>
      <button disabled={page === totalPages - 1} onClick={() => onChange(page + 1)}>
        ›
      </button>
    </nav>
  );
});
