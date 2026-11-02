import { useState } from 'react'
import { Link } from 'react-router-dom'
import AvisoError from '../comun/AvisoError'
import EstadoCarga from '../comun/EstadoCarga'
import { formatearFecha, formatearPrecio } from '../cotizaciones/cotizaciones'
import { MAXIMO_TOKENS_POR_ORDEN, OPERACIONES, validarOperacion } from './mercado'

function etiquetaBoton(tipo, numero) {
  const { verbo } = OPERACIONES[tipo]
  if (!Number.isInteger(numero) || numero < 1) return `${verbo} tokens`
  return `${verbo} ${numero} ${numero === 1 ? 'token' : 'tokens'}`
}

export default function PanelOperacion({ mercado, operacionInicial = 'COMPRA' }) {
  const { precio, disponibles, emitidos, fechaCotizacion } = mercado
  const [tipo, setTipo] = useState(OPERACIONES[operacionInicial] ? operacionInicial : 'COMPRA')
  const [cantidad, setCantidad] = useState('1')
  const [tocado, setTocado] = useState(false)

  if (mercado.cargando) return <section className="player-card market-panel"><EstadoCarga mensaje="Cargando tu saldo..." /></section>

  const tenencia = mercado.tenencia?.cantidad ?? 0
  const error = precio == null ? null : validarOperacion({ tipo, cantidad, precio, saldo: mercado.saldo, tenencia, disponibles })
  const numero = Number(cantidad)
  const total = Number.isFinite(numero) && precio != null ? numero * precio : 0

  const cambiarTipo = (nuevo) => {
    setTipo(nuevo)
    mercado.limpiarOperacion()
  }

  const enviar = async (event) => {
    event.preventDefault()
    setTocado(true)
    if (error || mercado.enviando) return
    const ok = await mercado.operar(tipo, numero)
    if (ok) {
      setCantidad('1')
      setTocado(false)
    }
  }

  return (
    <section className="player-card market-panel" aria-labelledby="titulo-operar">
      <div className="player-card-heading"><span id="titulo-operar">Operar</span></div>
      {mercado.error && (
        <>
          <AvisoError error={mercado.error} />
          <button className="btn ghost market-retry" type="button" onClick={mercado.reintentar}>Reintentar</button>
        </>
      )}
      <dl className="player-details market-balance">
        <dt>Saldo</dt><dd>{formatearPrecio(mercado.saldo)} créditos</dd>
        <dt>Tus tokens</dt>
        <dd>{tenencia}{mercado.tenencia?.precioPromedio != null && ` · promedio ${formatearPrecio(mercado.tenencia.precioPromedio)}`}</dd>
        {disponibles != null && <><dt>Disponibles</dt><dd>{disponibles}{emitidos != null && ` de ${emitidos}`}</dd></>}
      </dl>
      {precio == null || mercado.error
        ? <p className="market-note">No se pudieron obtener los datos del mercado, por ahora no se puede operar.</p>
        : (
          <form className="market-form" noValidate onSubmit={enviar}>
            <div className="market-type" role="group" aria-label="Tipo de operación">
              {Object.entries(OPERACIONES).map(([clave, { verbo }]) => (
                <button aria-pressed={tipo === clave} className="filter-chip" disabled={mercado.enviando} key={clave} type="button" onClick={() => cambiarTipo(clave)}>{verbo}</button>
              ))}
            </div>
            <label htmlFor="cantidad-tokens">Cantidad de tokens</label>
            <input
              aria-describedby="total-operacion error-operacion"
              aria-invalid={Boolean(tocado && error)}
              disabled={mercado.enviando}
              id="cantidad-tokens"
              inputMode="numeric"
              max={MAXIMO_TOKENS_POR_ORDEN}
              min="1"
              step="1"
              type="number"
              value={cantidad}
              onBlur={() => setTocado(true)}
              onChange={(event) => { setCantidad(event.target.value); mercado.limpiarOperacion() }}
            />
            <p className="market-total" id="total-operacion" aria-live="polite">
              Precio vigente {formatearPrecio(precio)} ({fechaCotizacion ? `al ${formatearFecha(fechaCotizacion)}` : 'precio inicial'}) · Total estimado <strong>{formatearPrecio(total)} créditos</strong>
            </p>
            <p className="market-error" id="error-operacion">{tocado && error}</p>
            <button className="btn market-submit" disabled={mercado.enviando} type="submit">
              {mercado.enviando ? 'Enviando...' : etiquetaBoton(tipo, numero)}
            </button>
          </form>
        )}
      {mercado.resultado && <p className="market-success" role="status">{mercado.resultado} <Link to="/operaciones">Ver historial</Link></p>}
      <AvisoError error={mercado.errorOperacion} />
    </section>
  )
}
