import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../hooks/useSesion'

export default function MenuPrincipal() {
  const navigate = useNavigate()
  const { usuario, cerrarSesion } = useSesion()
  const [selected, setSelected] = useState(0)
  const itemRefs = useRef([])
  const items = [
    usuario
      ? { label: 'Cerrar sesión', subtitle: 'Salir de la cuenta activa', action: 'logout' }
      : { label: 'Ingresar / Crear cuenta', subtitle: 'Acceder a Formula 11', action: 'access' },
    { label: 'Catálogo de jugadores', subtitle: 'Consultar el plantel', action: 'catalog' },
    { label: 'Ranking', subtitle: 'Comparar rendimiento', locked: true },
    { label: 'Mercado de tokens', subtitle: 'Invertir en jugadores', locked: true },
  ]

  useEffect(() => {
    itemRefs.current[selected]?.focus()
  }, [selected])

  function activate(item) {
    if (item.locked) return
    if (item.action === 'logout') {
      cerrarSesion()
      return
    }
    if (item.action === 'access') navigate('/ingresar')
    if (item.action === 'catalog') navigate('/catalogo')
  }

  function handleKeyDown(event) {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setSelected((current) => (current + 1) % items.length)
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setSelected((current) => (current - 1 + items.length) % items.length)
    }
    if (event.key === 'Enter') {
      event.preventDefault()
      activate(items[selected])
    }
    if (event.key === 'Escape') {
      event.preventDefault()
      navigate('/')
    }
  }

  return (
    <nav className="menu" aria-label="Menú principal" onKeyDown={handleKeyDown}>
      {items.map((item, index) => (
        <button
          className={`menu-item ${index === selected ? 'selected' : ''} ${item.locked ? 'locked' : ''}`.trim()}
          disabled={item.locked}
          key={item.label}
          onClick={() => {
            setSelected(index)
            activate(item)
          }}
          onFocus={() => setSelected(index)}
          ref={(element) => { itemRefs.current[index] = element }}
          type="button"
        >
          <span>
            <span className="menu-title">{item.label}</span>
            <span className="menu-subtitle">{item.subtitle}</span>
          </span>
          {item.locked && <span className="menu-tag">PRÓXIMAMENTE</span>}
        </button>
      ))}
    </nav>
  )
}
