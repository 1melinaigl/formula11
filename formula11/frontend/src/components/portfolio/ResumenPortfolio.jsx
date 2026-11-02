import { formatearConSigno, formatearPrecio, formatearVariacion } from '../cotizaciones/cotizaciones'

function claseSigno(valor) {
  if (!valor) return 'flat'
  return valor > 0 ? 'up' : 'down'
}

export default function ResumenPortfolio({ resumen }) {
  const tarjetas = [
    ['Valor total', resumen.total, 'Saldo + tokens a precio actual'],
    ['Saldo disponible', resumen.saldo, 'Créditos para comprar'],
    ['Invertido', resumen.invertido, 'Costo de los tokens que tenés'],
    ['Valor de tus tokens', resumen.valorActual, 'A la cotización vigente'],
  ]
  return (
    <ul className="quote-summary portfolio-summary" aria-label="Resumen del portfolio">
      {tarjetas.map(([titulo, valor, detalle]) => (
        <li className="quote-tile" key={titulo}>
          <span className="quote-strategy">{titulo}</span>
          <strong className="quote-price">{formatearPrecio(valor)} <small>créditos</small></strong>
          <span className="quote-date">{detalle}</span>
        </li>
      ))}
      <li className="quote-tile">
        <span className="quote-strategy">{resumen.ganancia < 0 ? 'Pérdida' : 'Ganancia'}</span>
        <strong className="quote-price">{formatearConSigno(resumen.ganancia)} <small>créditos</small></strong>
        <span className={`quote-change ${claseSigno(resumen.ganancia)}`}>
          {resumen.rendimiento == null ? 'Sin inversión' : `${formatearVariacion(resumen.rendimiento)} sobre lo invertido`}
        </span>
      </li>
    </ul>
  )
}
