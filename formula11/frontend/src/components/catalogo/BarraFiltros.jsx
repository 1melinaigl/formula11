import { posiciones } from './posiciones'

function Selector({ label, name, value, options, onChange }) {
  return (
    <div className="catalog-filter">
      <label htmlFor={`filtro-${name}`}>{label}</label>
      <select id={`filtro-${name}`} value={value} onChange={(event) => onChange(name, event.target.value)}>
        <option value="">Todas</option>
        {options.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    </div>
  )
}

export default function BarraFiltros({ filtros, opciones, actualizarFiltro, limpiarFiltros }) {
  const activos = Object.entries(filtros).filter(([, value]) => value.trim())
  return (
    <div className="catalog-toolbar">
      <div className="catalog-search">
        <label htmlFor="buscar-jugador">Buscar por nombre</label>
        <input id="buscar-jugador" value={filtros.nombre} onChange={(event) => actualizarFiltro('nombre', event.target.value)} placeholder="Nombre del jugador" />
      </div>
      <Selector label="Liga" name="liga" value={filtros.liga} options={opciones.ligas} onChange={actualizarFiltro} />
      <Selector label="Posición" name="posicion" value={filtros.posicion} options={Object.keys(posiciones)} onChange={actualizarFiltro} />
      <Selector label="Equipo" name="equipo" value={filtros.equipo} options={opciones.equipos} onChange={actualizarFiltro} />
      <button className="btn ghost catalog-clear" type="button" onClick={limpiarFiltros}>Limpiar</button>
      {activos.length > 0 && <div className="filter-chips" aria-label="Filtros activos">{activos.map(([name, value]) => <button className="filter-chip" key={name} type="button" onClick={() => actualizarFiltro(name, '')}>{value} ×</button>)}</div>}
    </div>
  )
}