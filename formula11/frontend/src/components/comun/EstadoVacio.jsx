export default function EstadoVacio({ titulo, detalle }) {
  return (
    <div className="empty-state" role="status">
      <h2>{titulo}</h2>
      {detalle && <p>{detalle}</p>}
    </div>
  )
}
