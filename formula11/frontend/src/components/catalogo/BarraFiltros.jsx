import { posiciones } from './posiciones'
import { CAMPOS_ORDEN } from '../../hooks/useCatalogo'

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

export default function BarraFiltros({ filtros, opciones, campoOrden, direccionOrden, actualizarFiltro, actualizarOrden, limpiarFiltros }) {
  const activos = Object.entries(filtros).filter(([, value]) => value.trim())
  return (
    <div className="catalog-toolbar">
      <div className="catalog-search">
        <label htmlFor="filtro-equipo">Equipo</label>
        <input id="filtro-equipo" value={filtros.equipo} onChange={(event) => actualizarFiltro('equipo', event.target.value)} placeholder="Buscar equipo" />
      </div>
      <Selector label="Liga" name="liga" value={filtros.liga} options={opciones.ligas} onChange={actualizarFiltro} />
      <Selector label="Posición" name="posicion" value={filtros.posicion} options={Object.values(posiciones)} onChange={actualizarFiltro} />
      <div className="catalog-filter">
        <label htmlFor="orden-campo">Ordenar por</label>
        <select id="orden-campo" value={campoOrden} onChange={(event) => actualizarOrden(event.target.value, direccionOrden)}>
          {CAMPOS_ORDEN.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </div>
      <div className="catalog-filter">
        <label htmlFor="orden-direccion">Dirección</label>
        <select id="orden-direccion" value={direccionOrden} onChange={(event) => actualizarOrden(campoOrden, event.target.value)}>
          <option value="asc">Ascendente</option>
          <option value="desc">Descendente</option>
        </select>
      </div>
      <button className="btn ghost catalog-clear" type="button" onClick={limpiarFiltros}>Limpiar</button>
      {activos.length > 0 && <div className="filter-chips" role="group" aria-label="Filtros activos">{activos.map(([name, value]) => <button aria-label={`Quitar filtro ${value}`} className="filter-chip" key={name} type="button" onClick={() => actualizarFiltro(name, '')}>{value} ×</button>)}</div>}
    </div>
  )
}