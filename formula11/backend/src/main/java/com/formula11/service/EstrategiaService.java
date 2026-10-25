package com.formula11.service;

import com.formula11.dto.*;
import com.formula11.model.Estrategia;
import com.formula11.persistence.EstrategiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class EstrategiaService {
    private final EstrategiaRepository repository;
    private final Clock clock;
    private final CotizacionCacheInvalidator cacheInvalidator;

    public EstrategiaService(EstrategiaRepository repository, Clock clock, CotizacionCacheInvalidator cacheInvalidator) {
        this.repository = repository; this.clock = clock; this.cacheInvalidator = cacheInvalidator;
    }

    public List<EstrategiaResponse> listar() {
        return repository.findAll().stream().sorted(Comparator.comparing(Estrategia::getCodigo))
                .map(this::toResponse).toList();
    }

    public List<EstrategiaResponse> versiones(String codigo) {
        List<Estrategia> result = repository.findByCodigoOrderByVersionDesc(codigo);
        if (result.isEmpty()) throw new NoSuchElementException("La estrategia no existe");
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EstrategiaResponse crearVersion(String codigo, NuevaVersionRequest request) {
        Estrategia actual = repository.findTopByCodigoOrderByVersionDesc(codigo)
                .orElseThrow(() -> new NoSuchElementException("La estrategia no existe"));
        validar(request);
        Estrategia nueva = new Estrategia(codigo, actual.getNombre(), actual.getVersion() + 1,
                request.pesos(), request.parametros(), false, Instant.now(clock));
        return toResponse(repository.save(nueva));
    }

    @Transactional
    public EstrategiaResponse activar(String codigo) {
        Estrategia seleccionada = repository.findTopByCodigoOrderByVersionDesc(codigo)
                .orElseThrow(() -> new NoSuchElementException("La estrategia no existe"));
        repository.findByActivaTrue().ifPresent(e -> e.activar(false));
        seleccionada.activar(true);
        cacheInvalidator.invalidate();
        return toResponse(repository.save(seleccionada));
    }

    private void validar(NuevaVersionRequest request) {
        if (request == null) throw new IllegalArgumentException("La versión es obligatoria");
        if (request.pesos().isEmpty() || request.parametros().isEmpty()) throw new IllegalArgumentException("Pesos y parámetros son obligatorios");
        Set<String> posiciones = Set.of("POR", "DEF", "MED", "DEL", "TODAS");
        Set<String> metricas = Set.of("goles", "asistencias", "tiros", "pasesClave", "regates",
                "entradas", "intercepciones", "amarillas", "rojas", "atajadas", "golesRecibidos",
                "vallaInvicta", "rating", "menosDe90Minutos", "titularidad", "suplencias");
        request.pesos().forEach((posicion, valores) -> {
            if (!posiciones.contains(posicion.toUpperCase(Locale.ROOT)) || valores == null || valores.isEmpty()) {
                throw new IllegalArgumentException("La posición de la estrategia no es válida");
            }
            valores.forEach((metrica, peso) -> {
                if (!metricas.contains(metrica) || peso == null || !Double.isFinite(peso)) {
                    throw new IllegalArgumentException("La métrica de la estrategia no es válida");
                }
            });
        });
        Double k = request.parametros().get("k");
        Double tope = request.parametros().get("topeVariacion");
        Double piso = request.parametros().get("piso");
        if (k == null || tope == null || piso == null || k < 0 || tope < 0 || piso < 0) throw new IllegalArgumentException("Los parámetros de la estrategia no son válidos");
    }

    private EstrategiaResponse toResponse(Estrategia e) {
        return new EstrategiaResponse(e.getCodigo(), e.getNombre(), e.getVersion(), e.isActiva(), e.getPesos(), e.getParametros(), e.getCreadaEn());
    }
}
