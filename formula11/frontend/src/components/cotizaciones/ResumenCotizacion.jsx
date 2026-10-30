import { formatearFecha, formatearPrecio, formatearVariacion, nombreEstrategia } from './cotizaciones'

function claseVariacion(variacion) {
  if (variacion === null || variacion === 0) return 'flat'
  return variacion > 0 ? 'up' : 'down'
}

export default function ResumenCotizacion({ resumen, estrategiaActiva }) {
  return (
    <ul className="quote-summary" aria-label="Cotización vigente">
      {resumen.map(({ estrategia, actual, variacion }) => (
        <li className="quote-tile" key={estrategia}>
          <span className="quote-strategy">
            <span>{nombreEstrategia(estrategia)}</span>
            {estrategia === estrategiaActiva && <small> · Activa</small>}
          </span>
          {actual ? (
            <>
              <strong className="quote-price">{formatearPrecio(actual.precio)} <small>créditos</small></strong>
              <span className={`quote-change ${claseVariacion(variacion)}`}>
                {variacion === null ? 'Sin cotización anterior' : `${formatearVariacion(variacion)} vs. semana anterior`}
              </span>
              <span className="quote-date">Al {formatearFecha(actual.fecha)}{actual.score != null && ` · Score ${Number(actual.score).toFixed(2)}`}</span>
            </>
          ) : (
            <span className="quote-change flat">Sin cotizaciones</span>
          )}
        </li>
      ))}
    </ul>
  )
}
