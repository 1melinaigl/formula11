package com.formula11.service.cotizaciones;

import com.formula11.model.Estrategia;
import com.formula11.persistence.cotizaciones.EstrategiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;

@Service
public class EstrategiaService {
    private static final Set<String> POSICIONES = Set.of("POR", "DEF", "MED", "DEL", "TODAS");
    private static final Set<String> METRICAS = Set.of("goles", "asistencias", "tiros", "pasesClave", "regates",
            "entradas", "intercepciones", "amarillas", "rojas", "atajadas", "golesRecibidos",
            "vallaInvicta", "rating", "menosDe90Minutos", "titularidad", "suplencias");

    private final EstrategiaRepository repository;
    private final Clock clock;
    private final CotizacionCacheInvalidator cacheInvalidator;

    public EstrategiaService(EstrategiaRepository repository, Clock clock, CotizacionCacheInvalidator cacheInvalidator) {
        this.repository = repository; this.clock = clock; this.cacheInvalidator = cacheInvalidator;
    }

    /**
     * Una fila por estrategia: su versión vigente (la más nueva), marcada como activa si ese código es el activo.
     * La marca se toma por código porque, al crear una versión de la estrategia activa, la fila activa sigue
     * siendo la anterior.
     */
    public List<EstrategiaResultado> listar() {
        List<Estrategia> todas = repository.findAll();
        Set<String> activas = new HashSet<>();
        todas.stream().filter(Estrategia::isActiva).map(Estrategia::getCodigo).forEach(activas::add);
        return Estrategia.vigentes(todas).stream().sorted(Comparator.comparing(Estrategia::getCodigo))
                .map(e -> EstrategiaResultado.de(e, activas.contains(e.getCodigo())))
                .toList();
    }

    public List<EstrategiaResultado> versiones(String codigo) {
        List<Estrategia> result = repository.findByCodigoOrderByVersionDesc(codigo);
        if (result.isEmpty()) throw new NoSuchElementException("La estrategia no existe");
        return result.stream().map(EstrategiaResultado::de).toList();
    }

    @Transactional
    public EstrategiaResultado crearVersion(String codigo, NuevaVersion version) {
        Estrategia actual = repository.findTopByCodigoOrderByVersionDesc(codigo)
                .orElseThrow(() -> new NoSuchElementException("La estrategia no existe"));
        validar(version);
        Estrategia nueva = new Estrategia(codigo, actual.getNombre(), actual.getVersion() + 1,
                version.pesos(), version.parametros(), false, clock.instant());
        return EstrategiaResultado.de(repository.save(nueva));
    }

    @Transactional
    public EstrategiaResultado activar(String codigo) {
        Estrategia seleccionada = repository.findTopByCodigoOrderByVersionDesc(codigo)
                .orElseThrow(() -> new NoSuchElementException("La estrategia no existe"));
        // La base admite una sola activa (ux_estrategia_activa): la anterior tiene que bajarse antes de marcar la nueva.
        repository.findByActivaTrue().ifPresent(actual -> {
            actual.activar(false);
            repository.saveAndFlush(actual);
        });
        seleccionada.activar(true);
        cacheInvalidator.invalidate();
        return EstrategiaResultado.de(repository.save(seleccionada));
    }

    private void validar(NuevaVersion request) {
        if (request == null) throw new IllegalArgumentException("La versión es obligatoria");
        if (request.pesos().isEmpty() || request.parametros().isEmpty()) throw new IllegalArgumentException("Pesos y parámetros son obligatorios");
        request.pesos().forEach(EstrategiaService::validarPesosDePosicion);
        Double k = request.parametros().get("k");
        Double tope = request.parametros().get("topeVariacion");
        Double piso = request.parametros().get("piso");
        if (k == null || tope == null || piso == null || k < 0 || tope < 0 || piso < 0) throw new IllegalArgumentException("Los parámetros de la estrategia no son válidos");
    }

    private static void validarPesosDePosicion(String posicion, Map<String, Double> pesos) {
        if (!POSICIONES.contains(posicion.toUpperCase(Locale.ROOT)) || pesos == null || pesos.isEmpty()) {
            throw new IllegalArgumentException("La posición de la estrategia no es válida");
        }
        pesos.forEach(EstrategiaService::validarPeso);
    }

    private static void validarPeso(String metrica, Double peso) {
        if (!METRICAS.contains(metrica) || peso == null || !Double.isFinite(peso)) {
            throw new IllegalArgumentException("La métrica de la estrategia no es válida");
        }
    }

}
