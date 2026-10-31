import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { COLOR_EJE, COLOR_GRILLA, EJE, ESTILO_TOOLTIP } from '../comun/estiloGraficos'
import { colorEstrategia, estrategiasPresentes, formatearFecha, formatearPrecio, nombreEstrategia, seriesPorFecha } from './cotizaciones'

export default function GraficoEvolucion({ cotizaciones, estrategiaActiva }) {
  const estrategias = estrategiasPresentes(cotizaciones)
  const datos = seriesPorFecha(cotizaciones)

  return (
    <figure className="quote-chart" aria-label="Evolución de la cotización">
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={datos} margin={{ top: 12, right: 16, bottom: 4, left: 0 }}>
          <CartesianGrid stroke={COLOR_GRILLA} strokeDasharray="3 3" />
          <XAxis dataKey="fecha" tick={EJE} tickFormatter={(fecha) => formatearFecha(fecha, { corta: true })} stroke={COLOR_EJE} />
          <YAxis tick={EJE} tickFormatter={formatearPrecio} stroke={COLOR_EJE} width={56} domain={['auto', 'auto']} />
          <Tooltip
            contentStyle={ESTILO_TOOLTIP}
            labelFormatter={(fecha) => formatearFecha(fecha)}
            formatter={(precio, estrategia) => [`${formatearPrecio(precio)} créditos`, nombreEstrategia(estrategia)]}
          />
          {estrategias.length > 1 && <Legend formatter={(estrategia) => `${nombreEstrategia(estrategia)}${estrategia === estrategiaActiva ? ' · Activa' : ''}`} />}
          {estrategias.map((estrategia, indice) => (
            <Line
              connectNulls
              dataKey={estrategia}
              dot={datos.length <= 20}
              isAnimationActive={false}
              key={estrategia}
              name={estrategia}
              stroke={colorEstrategia(estrategia, indice)}
              strokeWidth={2}
              type="monotone"
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </figure>
  )
}
