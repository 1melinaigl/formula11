package com.formula11.model.valuacion;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Continuity strategy based on the last six matches, with performance fallback. */
public final class Consistencia implements EstrategiaValuacion {
    private final RendimientoPosicion rendimiento;

    public Consistencia() {
        this(new RendimientoPosicion());
    }

    public Consistencia(RendimientoPosicion rendimiento) {
        this.rendimiento = Objects.requireNonNull(rendimiento);
    }

    @Override
    public ResultadoValuacion calcular(List<MetricaPartidoInput> metricas, LocalDate domingo,
                                       ParametrosEstrategia parametros) {
        ResultadoValuacion fallback = rendimiento.calcular(metricas, domingo, parametros);
        Map<Long, List<MetricaPartidoInput>> ventanas = metricas.stream()
                .filter(m -> !m.fecha().isAfter(domingo))
                .collect(Collectors.groupingBy(MetricaPartidoInput::jugadorId));
        Map<Long, Double> raw = new HashMap<>();
        Set<Long> elegibles = new HashSet<>();
        for (var entry : ventanas.entrySet()) {
            List<MetricaPartidoInput> partidos = entry.getValue().stream()
                    .sorted(Comparator.comparing(MetricaPartidoInput::fecha).reversed()).limit(6).toList();
            long jugados = partidos.stream().filter(MetricaPartidoInput::jugo).count();
            if (jugados < 4) continue;
            elegibles.add(entry.getKey());
            List<Double> ratings = partidos.stream().map(MetricaPartidoInput::rating)
                    .filter(Objects::nonNull).mapToDouble(x -> x.doubleValue()).boxed().toList();
            double promedio = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double desvio = Math.sqrt(ratings.stream().mapToDouble(v -> (v - promedio) * (v - promedio))
                    .average().orElse(0));
            double disponibilidad = jugados / 6d;
            double titularidad = partidos.stream().filter(m -> Boolean.TRUE.equals(m.titular())).count() / 6d;
            double suplencias = partidos.stream().filter(m -> Boolean.FALSE.equals(m.titular())).count() / 6d;
            double titularPeso = peso(parametros, "titularidad", 0.5);
            double suplentePeso = peso(parametros, "suplencias", -0.5);
            raw.put(entry.getKey(), disponibilidad * (promedio - parametros.lambda().doubleValue() * desvio)
                    + titularidad * titularPeso + suplencias * suplentePeso);
        }
        Map<Long, Double> scores = new HashMap<>();
        for (var e : raw.entrySet()) {
            List<Double> group = raw.entrySet().stream().filter(x -> samePosition(metricas, x.getKey(), e.getKey()))
                    .map(Map.Entry::getValue).toList();
            scores.put(e.getKey(), Normalizador.scoreFinal(e.getValue(), group));
        }
        // Players with insufficient history use the already normalized performance score.
        fallback.scores().forEach((id, score) -> scores.putIfAbsent(id, score));
        return new ResultadoValuacion(scores, raw, fallback.filasSinMinutos());
    }

    private static boolean samePosition(List<MetricaPartidoInput> all, long left, long right) {
        return position(all, left).equalsIgnoreCase(position(all, right));
    }
    private static String position(List<MetricaPartidoInput> all, long id) {
        return all.stream().filter(m -> m.jugadorId() == id).map(MetricaPartidoInput::posicion).findFirst().orElse("");
    }
    private static double peso(ParametrosEstrategia p, String name, double defaultValue) {
        return p.pesos().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name)
                || e.getKey().endsWith(":" + name)).mapToDouble(e -> e.getValue().doubleValue())
                .findFirst().orElse(defaultValue);
    }
}
