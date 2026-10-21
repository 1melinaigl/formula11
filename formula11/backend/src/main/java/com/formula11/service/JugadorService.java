package com.formula11.service;

import com.formula11.dto.JugadorResponse;
import com.formula11.dto.JugadoresPageResponse;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class JugadorService {
    private static final int MAX_SIZE = 100;
    private static final Set<String> SORT_FIELDS = Set.of("id", "nombre", "equipo", "liga", "posicion");
    private static final Map<String, Liga> LIGAS = java.util.Arrays.stream(Liga.values())
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    liga -> liga.getNombreLegible().toLowerCase(Locale.ROOT), liga -> liga));

    private final JugadorRepository jugadorRepository;

    public JugadorService(JugadorRepository jugadorRepository) {
        this.jugadorRepository = jugadorRepository;
    }

    public List<JugadorResponse> listar() {
        return jugadorRepository.findAllByOrderByNombreAsc().stream().map(JugadorResponse::from).toList();
    }

    public JugadoresPageResponse listar(int page, int size, String sort, String liga, String equipo, String posicion) {
        validarPaginacion(page, size);
        Pageable pageable = PageRequest.of(page, size, construirOrden(sort));
        Specification<Jugador> specification = (root, query, builder) -> null;
        specification = specification
                .and(filtrarLiga(liga))
                .and(filtrarTexto("equipo", equipo))
                .and(filtrarTexto("posicion", posicion));

        Page<Jugador> resultado = jugadorRepository.findAll(specification, pageable);
        return new JugadoresPageResponse(
                resultado.getContent().stream().map(JugadorResponse::from).toList(),
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    private static void validarPaginacion(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("El parámetro page no puede ser negativo");
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("El parámetro size debe estar entre 1 y 100");
        }
    }

    private static Sort construirOrden(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"));
        }

        String[] partes = sort.split(",", -1);
        if (partes.length != 2) throw new IllegalArgumentException("El parámetro sort debe tener formato campo,direccion");
        String campo = partes[0].trim().toLowerCase(Locale.ROOT);
        String direccion = partes[1].trim().toLowerCase(Locale.ROOT);
        if (!SORT_FIELDS.contains(campo)) throw new IllegalArgumentException("El campo de orden no es válido");
        if (!direccion.equals("asc") && !direccion.equals("desc")) {
            throw new IllegalArgumentException("La dirección de orden debe ser asc o desc");
        }

        return Sort.by(new Sort.Order(Sort.Direction.fromString(direccion), campo), Sort.Order.asc("id"));
    }

    private static Specification<Jugador> filtrarLiga(String valor) {
        if (valor == null) return null;
        String normalizado = normalizarFiltro(valor, "liga");
        Liga liga = LIGAS.get(normalizado.toLowerCase(Locale.ROOT));
        if (liga == null) throw new IllegalArgumentException("El filtro liga no es válido");
        return (root, query, builder) -> builder.equal(root.get("liga"), liga);
    }

    private static Specification<Jugador> filtrarTexto(String campo, String valor) {
        if (valor == null) return null;
        String normalizado = normalizarFiltro(valor, campo).toLowerCase(Locale.ROOT);
        return (root, query, builder) -> builder.like(builder.lower(root.get(campo)), "%" + escaparLike(normalizado) + "%", '\\');
    }

    private static String normalizarFiltro(String valor, String campo) {
        String normalizado = valor.trim();
        if (normalizado.isEmpty()) throw new IllegalArgumentException("El filtro " + campo + " no puede estar vacío");
        return normalizado;
    }

    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
