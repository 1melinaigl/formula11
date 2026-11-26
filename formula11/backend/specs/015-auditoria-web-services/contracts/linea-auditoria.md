# Contrato: línea del archivo de auditoría

La feature no agrega ni cambia endpoints HTTP. Lo que expone hacia afuera es el formato del
archivo `logs/auditoria.log`, que se usa en la demo y para buscar con `grep`.

## Archivo

- Ruta: `${app.auditoria.directorio:logs}/auditoria.log`.
- Rotación diaria: los días anteriores quedan como `auditoria.AAAA-MM-DD.log`.
- Se conservan 30 días.
- Una línea por llamada a un endpoint de `com.formula11.controller`.

## Formato

```text
<fecha ISO-8601 con zona> [corr:<correlationId>] usuario=<usuario> operacion=<Clase.metodo> parametros=<mapa> duracionMs=<n> resultado=<OK|ERROR(Excepcion)>
```

## Ejemplos

```text
2026-11-26T10:15:30.123-03:00 [corr:7f3c9a2e-1b4d-4f7a-9c11-2b8e5d6a0f11] usuario=ana@mail.com operacion=MercadoController.comprar parametros={request={"jugadorId":12,"cantidad":5}} duracionMs=18 resultado=OK
2026-11-26T10:16:02.540-03:00 [corr:c41d0b8e-2a7f-4e55-8d3a-91f0e6b2c7aa] usuario=anónimo operacion=UsuarioController.login parametros={request={"email":"ana@mail.com","password":"****"}} duracionMs=95 resultado=ERROR(CredencialesInvalidasException)
2026-11-26T10:17:44.001-03:00 [corr:0a9e4c3b-5d6f-4a21-b7e8-3c2d1f0e9b88] usuario=ana@mail.com operacion=PortfolioController.portfolio parametros={usuarioId=3} duracionMs=7 resultado=OK
```

## Garantías

- Nunca aparecen contraseñas ni tokens (ver [data-model.md](../data-model.md)).
- El correlation ID es el mismo que devuelve el header `X-Correlation-Id` y que aparece en
  el log general de esa llamada.
- Los pedidos que se cortan antes del controller (401, 403 y 400 de validación del cuerpo)
  no generan línea.
