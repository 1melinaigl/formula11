package com.formula11.unit.model;

import com.formula11.model.Senal;
import com.formula11.model.senales.DetectorSenales;
import com.formula11.model.senales.DetectorSenales.Candidato;
import com.formula11.model.senales.DetectorSenales.Detectada;
import com.formula11.model.senales.DetectorSenales.Parametros;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DetectorSenalesTest {
    private static final Parametros PARAMETROS = new Parametros(new BigDecimal("15"), new BigDecimal("0.05"), 5, 20);

    private static Candidato candidato(long id, String posicion, String precio, String score, String precioOtra) {
        return new Candidato(id, posicion, new BigDecimal(precio), new BigDecimal(score),
                precioOtra == null ? null : new BigDecimal(precioOtra));
    }

    private static List<Detectada> deTipo(List<Detectada> detectadas, Senal.Tipo tipo) {
        return detectadas.stream().filter(d -> d.tipo() == tipo).toList();
    }

    @Test
    void detectaEntreEstrategiasDesdeElUmbral() {
        List<Detectada> detectadas = DetectorSenales.detectar(List.of(
                candidato(1, "DEL", "1.0000", "0.5", "1.2000"),   // 20 %: señal
                candidato(2, "DEL", "1.0000", "0.5", "1.1500"),   // 15 % exacto: señal
                candidato(3, "DEL", "1.0000", "0.5", "1.1499"),   // 14,99 %: no
                candidato(4, "DEL", "1.0000", "0.5", "0.8000"),   // la otra vale menos: no
                candidato(5, "DEL", "1.0000", "0.5", null)), PARAMETROS);

        List<Detectada> entre = deTipo(detectadas, Senal.Tipo.ENTRE_ESTRATEGIAS);
        assertEquals(2, entre.size());
        assertEquals(1L, entre.get(0).jugadorId());
        assertEquals(new BigDecimal("20.00"), entre.get(0).brecha());
        assertEquals(new BigDecimal("1.0000"), entre.get(0).precio());
        assertEquals(new BigDecimal("1.2000"), entre.get(0).precioEstimado());
        assertEquals(2L, entre.get(1).jugadorId());
        assertEquals(new BigDecimal("15.00"), entre.get(1).brecha());
    }

    @Test
    void detectaPrecioAtrasadoConLaMedianaDeLosPares() {
        List<Candidato> candidatos = new ArrayList<>();
        candidatos.add(candidato(1, "MED", "1.0000", "0.80", null));
        // Cinco pares con score dentro de ±0,05: mediana 1.3000.
        candidatos.add(candidato(2, "MED", "1.2000", "0.78", null));
        candidatos.add(candidato(3, "MED", "1.2500", "0.82", null));
        candidatos.add(candidato(4, "MED", "1.3000", "0.85", null));
        candidatos.add(candidato(5, "MED", "1.4000", "0.75", null));
        candidatos.add(candidato(6, "MED", "1.5000", "0.80", null));
        // Fuera de la ventana o de otra posición: no son pares.
        candidatos.add(candidato(7, "MED", "9.0000", "0.20", null));
        candidatos.add(candidato(8, "DEL", "9.0000", "0.80", null));

        List<Detectada> atrasados = deTipo(DetectorSenales.detectar(candidatos, PARAMETROS), Senal.Tipo.PRECIO_ATRASADO);

        Detectada senal = atrasados.stream().filter(d -> d.jugadorId() == 1L).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1.3000"), senal.precioEstimado());
        assertEquals(new BigDecimal("30.00"), senal.brecha());
        assertTrue(atrasados.stream().noneMatch(d -> d.jugadorId() == 7L || d.jugadorId() == 8L));
    }

    @Test
    void sinSuficientesParesNoHayPrecioAtrasado() {
        List<Detectada> detectadas = DetectorSenales.detectar(List.of(
                candidato(1, "POR", "0.5000", "0.80", null),
                candidato(2, "POR", "2.0000", "0.80", null),
                candidato(3, "POR", "2.0000", "0.80", null),
                candidato(4, "POR", "2.0000", "0.80", null),
                candidato(5, "POR", "2.0000", "0.80", null)), PARAMETROS);

        assertTrue(deTipo(detectadas, Senal.Tipo.PRECIO_ATRASADO).isEmpty());
    }

    @Test
    void laMedianaDeUnaCantidadParEsElPromedioDeLosCentrales() {
        Parametros cuatroPares = new Parametros(new BigDecimal("15"), new BigDecimal("0.05"), 4, 20);
        List<Detectada> detectadas = DetectorSenales.detectar(List.of(
                candidato(1, "DEF", "1.0000", "0.50", null),
                candidato(2, "DEF", "1.2000", "0.50", null),
                candidato(3, "DEF", "1.3000", "0.50", null),
                candidato(4, "DEF", "1.4000", "0.50", null),
                candidato(5, "DEF", "1.6000", "0.50", null)), cuatroPares);

        Detectada senal = detectadas.stream().filter(d -> d.jugadorId() == 1L).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1.3500"), senal.precioEstimado());
        assertEquals(new BigDecimal("35.00"), senal.brecha());
    }

    @Test
    void recortaAlMaximoPorTipoPorBrechaDescendente() {
        Parametros dosPorTipo = new Parametros(new BigDecimal("15"), new BigDecimal("0.05"), 5, 2);
        List<Detectada> detectadas = DetectorSenales.detectar(List.of(
                candidato(1, "DEL", "1.0000", "0.5", "1.2000"),
                candidato(2, "DEL", "1.0000", "0.5", "1.5000"),
                candidato(3, "DEL", "1.0000", "0.5", "1.3000")), dosPorTipo);

        assertEquals(List.of(2L, 3L), detectadas.stream().map(Detectada::jugadorId).toList());
    }

    @Test
    void sinCandidatosNoHaySenales() {
        assertTrue(DetectorSenales.detectar(List.of(), PARAMETROS).isEmpty());
    }

    @Test
    void validaParametrosYCandidatos() {
        BigDecimal cero = BigDecimal.ZERO;
        BigDecimal quince = new BigDecimal("15");
        BigDecimal ventana = new BigDecimal("0.05");
        BigDecimal negativa = new BigDecimal("-0.01");
        BigDecimal uno = BigDecimal.ONE;
        BigDecimal medio = new BigDecimal("0.5");

        assertThrows(IllegalArgumentException.class, () -> new Parametros(cero, ventana, 5, 20));
        assertThrows(IllegalArgumentException.class, () -> new Parametros(quince, negativa, 5, 20));
        assertThrows(IllegalArgumentException.class, () -> new Parametros(quince, ventana, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> new Parametros(quince, ventana, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> new Candidato(1, "DEL", cero, medio, null));
        assertThrows(NullPointerException.class, () -> new Candidato(1, null, uno, medio, null));
    }
}
