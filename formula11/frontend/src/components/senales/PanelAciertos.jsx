import AvisoError from '../comun/AvisoError'
import EstadoCarga from '../comun/EstadoCarga'
import EstadoVacio from '../comun/EstadoVacio'
import { formatearFecha } from '../cotizaciones/cotizaciones'
import { formatearPorcentaje } from './senales'

function claseSigno(valor) {
  if (!valor) return 'flat'
  return valor > 0 ? 'up' : 'down'
}

function Variacion({ valor }) {
  return <span className={`quote-change ${claseSigno(valor)}`}>{formatearPorcentaje(valor)}</span>
}

function ContenidoAciertos({ aciertos }) {
  if (aciertos.semanas.length === 0) {
    return <EstadoVacio titulo="Sin semanas medidas" detalle="Todavía no hay semanas para medir: las señales se evalúan una semana después de generarse." />
  }

  return (
    <>
      <ul className="quote-summary aciertos-summary" aria-label="Resumen de aciertos">
        <li className="quote-tile">
          <span className="quote-strategy">Tasa de acierto</span>
          <strong className="quote-price">{formatearPorcentaje(aciertos.tasaAcierto, { signo: false })}</strong>
          <span className="quote-date">Señales que subieron más que el mercado la semana siguiente</span>
        </li>
        <li className="quote-tile">
          <span className="quote-strategy">Diferencia contra el mercado</span>
          <strong className="quote-price"><Variacion valor={aciertos.diferenciaPromedio} /></strong>
          <span className="quote-date">Suba promedio de los señalados menos la del mercado, en puntos</span>
        </li>
      </ul>
      <table className="ranking-table aciertos-table">
        <caption className="visually-hidden">Aciertos por semana</caption>
        <thead>
          <tr>
            <th scope="col">Semana</th>
            <th className="ranking-number" scope="col">Señales</th>
            <th className="ranking-number" scope="col">Señalados</th>
            <th className="ranking-number aciertos-optional" scope="col">Mercado</th>
            <th className="ranking-number" scope="col">Aciertos</th>
          </tr>
        </thead>
        <tbody>
          {aciertos.semanas.map((semana) => (
            <tr key={semana.fecha}>
              <td className="history-date">{formatearFecha(semana.fecha)}</td>
              <td className="ranking-number">{semana.senales}</td>
              <td className="ranking-number"><Variacion valor={semana.variacionSenalados} /></td>
              <td className="ranking-number aciertos-optional"><Variacion valor={semana.variacionMercado} /></td>
              <td className="ranking-number">{semana.aciertos} de {semana.senales}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}

// Tiene su propia carga y su propio error: un problema acá no oculta la lista de señales.
export default function PanelAciertos({ aciertos, cargando, error }) {
  return (
    <section aria-labelledby="titulo-aciertos" className="aciertos-panel">
      <h2 className="portfolio-subtitle" id="titulo-aciertos">Aciertos</h2>
      {cargando && <EstadoCarga mensaje="Cargando aciertos..." />}
      {!cargando && error && <AvisoError error={error} />}
      {!cargando && !error && <ContenidoAciertos aciertos={aciertos} />}
    </section>
  )
}
