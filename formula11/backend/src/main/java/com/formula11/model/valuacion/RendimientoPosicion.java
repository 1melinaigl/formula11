package com.formula11.model.valuacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Performance strategy: aggregate matches, standardize metrics, weight, normalize by position. */
public final class RendimientoPosicion implements EstrategiaValuacion {
    private static final Map<String, ToDoubleFunction<MetricaPartidoInput>> METRICS = Map.ofEntries(
            Map.entry("goles", m -> m.goles()), Map.entry("asistencias", m -> m.asistencias()),
            Map.entry("tiros", m -> m.tiros()), Map.entry("pasesClave", m -> m.pasesClave()),
            Map.entry("regates", m -> m.regates()), Map.entry("entradas", m -> m.entradas()),
            Map.entry("intercepciones", m -> m.intercepciones()), Map.entry("amarillas", m -> m.amarillas()),
            Map.entry("rojas", m -> m.rojas()), Map.entry("atajadas", m -> m.atajadas()),
            Map.entry("golesRecibidos", m -> m.golesRecibidos()), Map.entry("vallaInvicta", m -> m.vallaInvicta()),
            Map.entry("rating", m -> m.rating() == null ? Double.NaN : m.rating().doubleValue()),
            Map.entry("menosDe90Minutos", m -> m.minutos() != null && m.minutos() < 90 ? 1d : 0d));

    @Override
    public ResultadoValuacion calcular(List<MetricaPartidoInput> metricas, LocalDate fecha,
                                       ParametrosEstrategia parametros) {
        Map<Long, List<MetricaPartidoInput>> porJugador = metricas.stream()
                .filter(MetricaPartidoInput::jugo).collect(Collectors.groupingBy(MetricaPartidoInput::jugadorId));
        Map<Long, String> posiciones = porJugador.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, e -> e.getValue().get(0).posicion().toUpperCase(Locale.ROOT)));
        Map<Long, Double> brutos = new HashMap<>();
        for (var entry : porJugador.entrySet()) {
            String posicion = posiciones.get(entry.getKey());
            Map<String, Double> promedio = promedios(entry.getValue());
            double bruto = 0;
            List<Map.Entry<String, BigDecimal>> pesos = parametros.pesos().entrySet().stream()
                    .filter(p -> aplica(p.getKey(), posicion)).toList();
            boolean ratingAusente = !promedio.containsKey("rating");
            double ratingRedistribuible = ratingAusente ? pesos.stream()
                    .filter(p -> metricName(p.getKey()).equals("rating") && p.getValue().signum() > 0)
                    .mapToDouble(p -> p.getValue().doubleValue()).sum() : 0;
            double sumaPositivosDisponibles = pesos.stream()
                    .filter(p -> p.getValue().signum() > 0 && !metricName(p.getKey()).equals("rating")
                            && promedio.containsKey(metricName(p.getKey())))
                    .mapToDouble(p -> p.getValue().doubleValue()).sum();
            for (var peso : pesos) {
                String metrica = metricName(peso.getKey());
                if (!METRICS.containsKey(metrica) || !promedio.containsKey(metrica)) continue;
                List<Double> grupo = porJugador.entrySet().stream()
                        .filter(e -> posiciones.get(e.getKey()).equalsIgnoreCase(posicion))
                        .map(e -> promedios(e.getValue()).get(metrica))
                        .filter(Objects::nonNull).filter(v -> !Double.isNaN(v)).toList();
                double pesoEfectivo = peso.getValue().doubleValue();
                if (ratingAusente && !metrica.equals("rating") && pesoEfectivo > 0
                        && sumaPositivosDisponibles > 0) {
                    pesoEfectivo += ratingRedistribuible * pesoEfectivo / sumaPositivosDisponibles;
                }
                bruto += pesoEfectivo * Normalizador.zScore(promedio.get(metrica), grupo);
            }

            brutos.put(entry.getKey(), bruto);
        }
        Map<Long, Double> scores = new HashMap<>();
        for (var e : brutos.entrySet()) {
            List<Double> grupo = brutos.entrySet().stream()
                    .filter(x -> posiciones.get(x.getKey()).equalsIgnoreCase(posiciones.get(e.getKey())))
                    .map(Map.Entry::getValue).toList();
            scores.put(e.getKey(), Normalizador.scoreFinal(e.getValue(), grupo));
        }
        Set<Long> excluidas = metricas.stream().filter(m -> !m.jugo()).map(MetricaPartidoInput::jugadorId)
                .collect(Collectors.toSet());
        return new ResultadoValuacion(scores, brutos, excluidas);
    }

    private static boolean aplica(String clave, String posicion) {
        String[] partes = clave.split(":", 2);
        return partes.length == 1 || partes[0].equalsIgnoreCase(posicion)
                || partes[0].equalsIgnoreCase("TODAS");
    }

    private static String metricName(String clave) {
        String[] partes = clave.split(":", 2);
        return partes.length == 2 ? partes[1] : partes[0];
    }

    private static Map<String, Double> promedios(List<MetricaPartidoInput> values) {
        Map<String, List<Double>> valuesByMetric = new HashMap<>();
        for (var value : values) {
            for (var metric : METRICS.entrySet()) {
                double number = metric.getValue().applyAsDouble(value);
                if (!Double.isNaN(number)) valuesByMetric.computeIfAbsent(metric.getKey(), k -> new ArrayList<>()).add(number);
            }
        }
        return valuesByMetric.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)));
    }
}
