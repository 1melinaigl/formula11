import { useMemo } from 'react'
import { useSesion } from '../hooks/useSesion'
import MenuPrincipal from '../components/navegacion/MenuPrincipal'

export default function InicioPage() {
  const { usuario } = useSesion()
  const saludo = useMemo(() => (usuario ? `Sesión activa: ${usuario.nombre}` : 'Ingresá para descubrir el plantel'), [usuario])

  return (
    <section className="screen">
      <MenuPrincipal />
      <div className="hero-copy">
        <h1>Formula <em>11</em></h1>
        <p>{saludo}</p>
      </div>
    </section>
  )
}
