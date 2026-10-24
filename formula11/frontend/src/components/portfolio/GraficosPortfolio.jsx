import { Bar, BarChart, CartesianGrid, Cell, LabelList, Legend, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { COLOR_EJE, COLOR_GRILLA, EJE, ESTILO_TOOLTIP } from '../comun/estiloGraficos'
import { formatearConSigno, formatearFecha, formatearPrecio } from '../cotizaciones/cotizaciones'
import { COLORES } from './portfolio'

const SERIES_EVOLUCION = { valor: 'Valor actual', invertido: 'Invertido' }
const formatoPorcentaje = new Intl.NumberFormat('es-AR', { style: 'percent', maximumFractionDigits: 0 })
const ALTO_FILA = 34

function altoBarras(filas) {
  return Math.max(120, filas * ALTO_FILA + 40)
}

// La etiqueta va a la derecha del extremo derecho de la barra (en las negativas, a la derecha del cero),
// así nunca pisa los nombres del eje, a cualquier ancho.
function EtiquetaGanancia({ x, y, width, height, value }) {
  return (
    <text dy="0.35em" fill="#f6f1dc" fontSize={12} textAnchor="start" x={Math.max(x, x + width) + 6} y={y + height / 2}>
      {formatearConSigno(value)}
    </text>
  )
}

const DOMINIO_GANANCIAS = [(minimo) => Math.min(0, minimo * 1.1), (maximo) => Math.max(0, maximo * 1.1)]

function Tarjeta({ titulo, children }) {
  return (
    <figure className="player-card portfolio-chart">
      <figcaption className="player-card-heading"><span>{titulo}</span></figcaption>
      {children}
    </figure>
  )
}

const TITULO_EVOLUCION = 'Valor del portfolio vs. invertido'

export function AvisoPocaHistoria() {
  return (
    <Tarjeta titulo={TITULO_EVOLUCION}>
      <p className="market-note portfolio-note">Todavía no hay suficiente historia para mostrar la evolución (hace falta al menos una semana más de cotizaciones).</p>
    </Tarjeta>
  )
}

// Alternativa textual del gráfico de evolución para lectores de pantalla.
function TablaEvolucion({ evolucion }) {
  return (
    <table className="visually-hidden">
      <caption>Evolución semanal</caption>
      <thead>
        <tr><th scope="col">Fecha</th><th scope="col">Valor</th><th scope="col">Invertido</th></tr>
      </thead>
      <tbody>
        {evolucion.map((punto) => (
          <tr key={punto.fecha}>
            <td>{formatearFecha(punto.fecha)}</td>
            <td>{formatearPrecio(punto.valor)}</td>
            <td>{formatearPrecio(punto.invertido)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function GraficoEvolucionPortfolio({ evolucion }) {
  return (
    <Tarjeta titulo={TITULO_EVOLUCION}>
      <TablaEvolucion evolucion={evolucion} />
      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={evolucion} margin={{ top: 16, right: 16, bottom: 4, left: 0 }}>
          <CartesianGrid stroke={COLOR_GRILLA} strokeDasharray="3 3" />
          <XAxis dataKey="fecha" tick={EJE} tickFormatter={(fecha) => formatearFecha(fecha, { corta: true })} stroke={COLOR_EJE} />
          <YAxis tick={EJE} tickFormatter={formatearPrecio} stroke={COLOR_EJE} width={56} domain={['auto', 'auto']} />
          <Tooltip
            contentStyle={ESTILO_TOOLTIP}
            labelFormatter={(fecha) => formatearFecha(fecha)}
            formatter={(valor, serie) => [`${formatearPrecio(valor)} créditos`, SERIES_EVOLUCION[serie]]}
          />
          <Legend formatter={(serie) => SERIES_EVOLUCION[serie]} />
          <Line dataKey="valor" dot={evolucion.length <= 20} isAnimationActive={false} stroke={COLORES.valor} strokeWidth={2} type="monotone" />
          <Line dataKey="invertido" dot={false} isAnimationActive={false} stroke={COLORES.invertido} strokeDasharray="6 4" strokeWidth={2} type="stepAfter" />
        </LineChart>
      </ResponsiveContainer>
    </Tarjeta>
  )
}

export function GraficoComposicion({ filas }) {
  return (
    <Tarjeta titulo="Composición por jugador">
      <ResponsiveContainer width="100%" height={altoBarras(filas.length)}>
        <BarChart data={filas} layout="vertical" margin={{ top: 8, right: 48, bottom: 4, left: 8 }} barCategoryGap={6}>
          <XAxis hide type="number" />
          <YAxis dataKey="nombre" tick={EJE} type="category" width={120} stroke={COLOR_EJE} />
          <Tooltip
            contentStyle={ESTILO_TOOLTIP}
            cursor={{ fill: 'rgba(240, 212, 106, .1)' }}
            formatter={(valor, _serie, { payload }) => [`${formatearPrecio(valor)} créditos (${formatoPorcentaje.format(payload.porcentaje)})`, 'Valor actual']}
          />
          <Bar dataKey="valor" fill={COLORES.valor} isAnimationActive={false} radius={[0, 4, 4, 0]}>
            <LabelList dataKey="porcentaje" fill="#f6f1dc" fontSize={12} formatter={(porcentaje) => formatoPorcentaje.format(porcentaje)} position="right" />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </Tarjeta>
  )
}

export function GraficoGanancias({ filas }) {
  return (
    <Tarjeta titulo="Ganancia o pérdida por jugador">
      <ResponsiveContainer width="100%" height={altoBarras(filas.length)}>
        <BarChart data={filas} layout="vertical" margin={{ top: 8, right: 48, bottom: 4, left: 8 }} barCategoryGap={6}>
          <CartesianGrid horizontal={false} stroke={COLOR_GRILLA} strokeDasharray="3 3" />
          <XAxis tick={EJE} tickFormatter={formatearPrecio} type="number" stroke={COLOR_EJE} domain={DOMINIO_GANANCIAS} />
          <YAxis dataKey="nombre" tick={EJE} type="category" width={120} stroke={COLOR_EJE} />
          <ReferenceLine stroke={COLOR_EJE} x={0} />
          <Tooltip
            contentStyle={ESTILO_TOOLTIP}
            cursor={{ fill: 'rgba(240, 212, 106, .1)' }}
            formatter={(ganancia) => [`${formatearConSigno(ganancia)} créditos`, ganancia < 0 ? 'Pérdida' : 'Ganancia']}
          />
          <Bar dataKey="ganancia" isAnimationActive={false} radius={2}>
            {filas.map((fila) => <Cell fill={fila.ganancia < 0 ? COLORES.perdida : COLORES.ganancia} key={fila.nombre} />)}
            <LabelList content={EtiquetaGanancia} dataKey="ganancia" />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </Tarjeta>
  )
}
