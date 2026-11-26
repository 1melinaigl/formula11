package com.formula11.unit.observabilidad;

import com.formula11.observabilidad.LineaAuditoria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineaAuditoriaTest {

    @Test
    void formateaUnaLlamadaExitosaEnUnaSolaLinea() {
        LineaAuditoria linea = new LineaAuditoria(
                "ana@mail.com", "PortfolioController.portfolio", "{usuarioId=3}", 7, "OK");

        assertEquals(
                "usuario=ana@mail.com operacion=PortfolioController.portfolio parametros={usuarioId=3} duracionMs=7 resultado=OK",
                linea.formatear());
    }

    @Test
    void formateaUnaLlamadaConError() {
        LineaAuditoria linea = new LineaAuditoria(
                "anónimo", "UsuarioController.login", "{request={\"email\":\"ana@mail.com\"}}", 95,
                LineaAuditoria.resultadoError(new IllegalStateException("x")));

        assertEquals(
                "usuario=anónimo operacion=UsuarioController.login parametros={request={\"email\":\"ana@mail.com\"}} "
                        + "duracionMs=95 resultado=ERROR(IllegalStateException)",
                linea.formatear());
    }

    @Test
    void sinParametrosMuestraLlavesVacias() {
        LineaAuditoria linea = new LineaAuditoria("anónimo", "JugadorController.listar", null, 0, LineaAuditoria.OK);

        assertEquals(
                "usuario=anónimo operacion=JugadorController.listar parametros={} duracionMs=0 resultado=OK",
                linea.formatear());
    }

    @Test
    void rechazaUsuarioVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new LineaAuditoria(" ", "JugadorController.listar", "{}", 1, LineaAuditoria.OK));
    }

    @Test
    void rechazaDuracionNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> new LineaAuditoria("anónimo", "JugadorController.listar", "{}", -1, LineaAuditoria.OK));
    }
}
