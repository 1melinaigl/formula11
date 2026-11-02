import { useState } from 'react'
import { useActivarEstrategia } from '../../hooks/useActivarEstrategia'
import AvisoError from '../comun/AvisoError'
import { ESTRATEGIAS } from '../cotizaciones/cotizaciones'

// Se elige y después se confirma con el botón, porque el cambio afecta a todo el mercado.
export default function SelectorEstrategia({ estrategiaActiva, alActivar }) {
  const [elegida, setElegida] = useState(null)
  const { enviando, error, activar } = useActivarEstrategia(alActivar)
  const valor = elegida ?? estrategiaActiva

  function confirmar(event) {
    event.preventDefault()
    activar(valor)
  }

  return (
    <form className="ranking-strategy" aria-label="Cambiar la estrategia activa" onSubmit={confirmar}>
      <div className="catalog-filter">
        <label htmlFor="ranking-estrategia">Estrategia del mercado</label>
        <select id="ranking-estrategia" value={valor} onChange={(event) => setElegida(event.target.value)}>
          {Object.entries(ESTRATEGIAS).map(([codigo, { nombre }]) => <option key={codigo} value={codigo}>{nombre}</option>)}
        </select>
      </div>
      <button className="btn" type="submit" disabled={enviando || valor === estrategiaActiva}>{enviando ? 'Activando...' : 'Activar'}</button>
      <p className="ranking-strategy-note">Cambia los precios y el valor de los portfolios de todos los usuarios al instante.</p>
      <AvisoError error={error} />
    </form>
  )
}
