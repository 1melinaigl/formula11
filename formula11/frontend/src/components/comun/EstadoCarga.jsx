export default function EstadoCarga({ mensaje = 'Cargando...' }) {
  return (
    <p className="status-message" role="status" aria-live="polite">
      {mensaje}
    </p>
  )
}
