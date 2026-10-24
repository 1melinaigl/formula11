import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { colorEstrategia, estrategiasPresentes, formatearFecha, formatearPrecio, nombreEstrategia, seriesPorFecha } from './cotizaciones'

const EJE = { fill: '#b9d4b0', fontSize: 12 }

export default function GraficoEvolucion({ cotizaciones }) {
  const estrategias = estrategiasPresentes(cotizaciones)
  const datos = seriesPorFecha(cotizaciones)

  return (
    <figure className="quote-chart" aria-label="Evolución de la cotización">
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={datos} margin={{ top: 12, right: 16, bottom: 4, left: 0 }}>
          <CartesianGrid stroke="rgba(220, 240, 200, .15)" strokeDasharray="3 3" />
          <XAxis dataKey="fecha" tick={EJE} tickFormatter={(fecha) => formatearFecha(fecha, { corta: true })} stroke="#b9d4b0" />
          <YAxis tick={EJE} tickFormatter={formatearPrecio} stroke="#b9d4b0" width={56} domain={['auto', 'auto']} />
          <Tooltip
            contentStyle={{ background: '#06210f', border: '1px solid #f4e9b8', borderRadius: 0 }}
            labelFormatter={(fecha) => formatearFecha(fecha)}
            formatter={(precio, estrategia) => [`${formatearPrecio(precio)} créditos`, nombreEstrategia(estrategia)]}
          />
          {estrategias.length > 1 && <Legend formatter={nombreEstrategia} />}
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
