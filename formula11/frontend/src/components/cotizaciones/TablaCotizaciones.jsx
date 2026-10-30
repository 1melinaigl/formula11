import { formatearFecha, formatearPrecio, nombreEstrategia } from './cotizaciones'

export default function TablaCotizaciones({ cotizaciones }) {
  const filas = [...cotizaciones].reverse()
  return (
    <details className="quote-history">
      <summary>Ver historial completo</summary>
      <table>
        <thead>
          <tr><th scope="col">Fecha</th><th scope="col">Estrategia</th><th scope="col">Precio</th><th scope="col">Score</th></tr>
        </thead>
        <tbody>
          {filas.map((cotizacion) => (
            <tr key={`${cotizacion.fecha}-${cotizacion.estrategia}`}>
              <td>{formatearFecha(cotizacion.fecha)}</td>
              <td>{nombreEstrategia(cotizacion.estrategia)}{cotizacion.version != null && ` v${cotizacion.version}`}</td>
              <td>{formatearPrecio(cotizacion.precio)}</td>
              <td>{cotizacion.score == null ? '—' : Number(cotizacion.score).toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  )
}
