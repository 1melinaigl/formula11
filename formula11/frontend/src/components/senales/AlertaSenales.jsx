import { useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAlertasSenales } from '../../hooks/useAlertasSenales'
import { TIPOS_SENAL, formatearPorcentaje } from './senales'

const MAXIMO_DESTACADAS = 3

function titulo(cantidad) {
  return cantidad === 1 ? '1 oportunidad nueva' : `${cantidad} oportunidades nuevas`
}

// Alerta global: aparece sobre cualquier pantalla sin mover el foco. En /senales no se muestra,
// porque la lista ya tiene las señales a la vista (y las marca como vistas).
export default function AlertaSenales() {
  const { nuevas, cerrar, verSenales } = useAlertasSenales()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const alerta = useRef(null)

  if (nuevas.length === 0 || pathname === '/senales') return null

  // Si el foco estaba en la alerta, al desaparecer quedaría en el body: se pasa al contenido principal.
  const devolverFoco = (accion) => () => {
    const teniaFoco = alerta.current?.contains(document.activeElement)
    accion()
    if (teniaFoco) document.getElementById('contenido')?.focus()
  }
  const cerrarAlerta = devolverFoco(cerrar)
  const irASenales = devolverFoco(() => { verSenales(); navigate('/senales') })

  const cerrarConEscape = (event) => {
    if (event.key !== 'Escape') return
    // Sin esto, el Escape también dispararía el "volver" de la página de abajo.
    event.stopPropagation()
    cerrarAlerta()
  }

  return (
    <section aria-label="Oportunidades nuevas" className="panel signal-alert" ref={alerta} role="status" onKeyDown={cerrarConEscape}>
      <p className="signal-alert-title">{titulo(nuevas.length)}</p>
      <ul className="signal-alert-list">
        {nuevas.slice(0, MAXIMO_DESTACADAS).map((senal) => (
          <li key={senal.id}>
            <strong>{senal.jugador.nombre}</strong>
            <span>{TIPOS_SENAL[senal.tipo].nombre}</span>
            <span className="quote-change up">{formatearPorcentaje(senal.brecha)}</span>
          </li>
        ))}
      </ul>
      <div className="signal-alert-actions">
        <button className="btn signal-alert-button" type="button" onClick={irASenales}>Ver señales</button>
        <button className="btn ghost signal-alert-button" type="button" onClick={cerrarAlerta}>Cerrar</button>
      </div>
    </section>
  )
}
