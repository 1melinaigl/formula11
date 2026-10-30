export default function PaginacionCatalogo({ page, totalPages, cambiarPagina, etiqueta = 'Paginación del catálogo' }) {
  if (totalPages === 0) return null

  return (
    <nav className="catalog-pagination" aria-label={etiqueta}>
      <button className="btn ghost" type="button" onClick={() => cambiarPagina(page - 1)} disabled={page === 0}>
        Anterior
      </button>
      <span aria-live="polite">Página {page + 1} de {totalPages}</span>
      <button className="btn ghost" type="button" onClick={() => cambiarPagina(page + 1)} disabled={page >= totalPages - 1}>
        Siguiente
      </button>
    </nav>
  )
}
