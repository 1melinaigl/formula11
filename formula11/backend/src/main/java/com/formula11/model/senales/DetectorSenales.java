package com.formula11.model.senales;

import com.formula11.model.Senal;
import com.formula11.model.VariacionCotizacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Detecta jugadores subvaluados en una semana. Regla de dominio pura: sin Spring ni base de datos.
 * <ul>
 *   <li>ENTRE_ESTRATEGIAS: la otra estrategia valora al jugador al menos un umbral más que la activa.</li>
 *   <li>PRECIO_ATRASADO: la mediana de precios de sus pares (misma posición, score parecido) supera su precio
 *   al menos en el umbral.</li>
 * </ul>
 */
public final class DetectorSenales {
    private static final BigDecimal DOS = BigDecimal.valueOf(2);
    private static final Comparator<Detectada> POR_BRECHA = Comparator
            .comparing(Detectada::brecha, Comparator.reverseOrder())
            .thenComparingLong(Detectada::jugadorId);

    private DetectorSenales() {
    }

    /** Jugador que jugó en la semana, con su precio y score de la estrategia activa y su precio en la otra. */
    public record Candidato(long jugadorId, String posicion, BigDecimal precio, BigDecimal score,
                            BigDecimal precioOtraEstrategia) {
        public Candidato {
            Objects.requireNonNull(posicion, "La posición es obligatoria");
            if (precio == null || precio.signum() <= 0) {
                throw new IllegalArgumentException("El precio debe ser positivo");
            }
            Objects.requireNonNull(score, "El score es obligatorio");
        }
    }

    public record Parametros(BigDecimal umbral, BigDecimal ventanaScore, int minimoPares, int maximoPorTipo) {
        public Parametros {
            if (umbral == null || umbral.signum() <= 0) {
                throw new IllegalArgumentException("El umbral debe ser positivo");
            }
            if (ventanaScore == null || ventanaScore.signum() < 0) {
                throw new IllegalArgumentException("La ventana de score no puede ser negativa");
            }
            if (minimoPares < 1 || maximoPorTipo < 1) {
                throw new IllegalArgumentException("El mínimo de pares y el máximo por tipo deben ser al menos 1");
            }
        }
    }

    public record Detectada(long jugadorId, Senal.Tipo tipo, BigDecimal brecha, BigDecimal precio,
                            BigDecimal precioEstimado) {
    }

    public static List<Detectada> detectar(List<Candidato> candidatos, Parametros parametros) {
        List<Detectada> entreEstrategias = new ArrayList<>();
        List<Detectada> atrasados = new ArrayList<>();
        Map<String, List<Candidato>> porPosicion = candidatos.stream()
                .collect(Collectors.groupingBy(Candidato::posicion));
        for (Candidato candidato : candidatos) {
            detectarEntreEstrategias(candidato, parametros).ifPresent(entreEstrategias::add);
            detectarPrecioAtrasado(candidato, porPosicion.getOrDefault(candidato.posicion(), List.of()), parametros)
                    .ifPresent(atrasados::add);
        }
        List<Detectada> resultado = new ArrayList<>(recortar(entreEstrategias, parametros.maximoPorTipo()));
        resultado.addAll(recortar(atrasados, parametros.maximoPorTipo()));
        return resultado;
    }

    private static Optional<Detectada> detectarEntreEstrategias(Candidato candidato, Parametros parametros) {
        BigDecimal otra = candidato.precioOtraEstrategia();
        if (otra == null || otra.compareTo(candidato.precio()) <= 0) {
            return Optional.empty();
        }
        return siSuperaUmbral(candidato, Senal.Tipo.ENTRE_ESTRATEGIAS, otra, parametros);
    }

    private static Optional<Detectada> detectarPrecioAtrasado(Candidato candidato, List<Candidato> mismaPosicion,
                                                                      Parametros parametros) {
        List<BigDecimal> preciosPares = mismaPosicion.stream()
                .filter(par -> par.jugadorId() != candidato.jugadorId())
                .filter(par -> par.score().subtract(candidato.score()).abs().compareTo(parametros.ventanaScore()) <= 0)
                .map(Candidato::precio)
                .sorted()
                .toList();
        if (preciosPares.size() < parametros.minimoPares()) {
            return Optional.empty();
        }
        BigDecimal estimado = mediana(preciosPares);
        if (estimado.compareTo(candidato.precio()) <= 0) {
            return Optional.empty();
        }
        return siSuperaUmbral(candidato, Senal.Tipo.PRECIO_ATRASADO, estimado, parametros);
    }

    private static Optional<Detectada> siSuperaUmbral(Candidato candidato, Senal.Tipo tipo,
                                                                BigDecimal estimado, Parametros parametros) {
        BigDecimal brecha = VariacionCotizacion.porcentaje(candidato.precio(), estimado);
        if (brecha == null || brecha.compareTo(parametros.umbral()) < 0) {
            return Optional.empty();
        }
        return Optional.of(new Detectada(candidato.jugadorId(), tipo, brecha,
                candidato.precio().setScale(4, RoundingMode.HALF_UP), estimado.setScale(4, RoundingMode.HALF_UP)));
    }

    /** Mediana de una lista ordenada; con cantidad par, el promedio de los dos centrales con 4 decimales. */
    static BigDecimal mediana(List<BigDecimal> ordenados) {
        int medio = ordenados.size() / 2;
        if (ordenados.size() % 2 == 1) {
            return ordenados.get(medio).setScale(4, RoundingMode.HALF_UP);
        }
        return ordenados.get(medio - 1).add(ordenados.get(medio)).divide(DOS, 4, RoundingMode.HALF_UP);
    }

    private static List<Detectada> recortar(List<Detectada> detectadas, int maximo) {
        return detectadas.stream().sorted(POR_BRECHA).limit(maximo).toList();
    }
}
