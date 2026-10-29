# Quickstart: verificación manual del scraper automático

Esta guía verifica manualmente el adapter automático de WhoScored. No forma
parte de los tests automáticos: requiere red, Chrome real, un perfil
persistente y una cuenta `SUPERUSUARIO`.

## 1. Requisitos

- Java 21.
- Maven.
- Chrome instalado.
- Acceso de red a WhoScored.
- Credenciales de `SUPERUSUARIO` configuradas según la configuración existente
  de la aplicación.
- Playwright para Java declarado por el backend y sus browsers/dependencias
  instalados según la versión configurada.

No usar `app.whoscored.modo=archivos` para esta verificación; ese modo es para
tests y respaldo offline.

## 2. Configuración

Configurar, mediante `application.yml`, un perfil local o variables equivalentes,
como mínimo:

```yaml
app:
  whoscored:
    modo: automatico
    ligas:
      PREMIER_LEAGUE: 25544
      LA_LIGA: 25662
      BUNDESLIGA: 25666
      SERIE_A: 25518
      LIGUE_1: 25554
    inicio-temporada: 2026-08-01
    zona-fuente: Europe/London
    perfil-dir: C:/ruta/segura/whoscored-perfil
    headless: false
    pausa-ms: 3000
    timeout-ms: 90000
    reintentos: 30
    carpeta-paginas: C:/ruta/opcional/whoscored
```

Los nombres exactos de las propiedades deben coincidir con
`WhoScoredProperties`. El directorio de perfil debe ser propio de esta
verificación y no debe compartirse con una sesión interactiva de Chrome que
esté abierta.

La aplicación debe usar:

- Chrome (`channel: chrome`);
- perfil persistente;
- sin `--enable-automation`;
- `--disable-blink-features=AutomationControlled`;
- `navigator.webdriver` indefinido;
- un único navegador reutilizado durante la carga.

## 3. Iniciar el backend

Desde `formula11/backend` iniciar la aplicación con el perfil local elegido y
esperar a que el backend esté disponible en `http://localhost:8080`.

Antes de una carga automática, verificar que no haya otra ejecución en curso.
Una segunda carga responde `409`.

## 4. Obtener un JWT de superusuario

Usar las credenciales del superusuario configurado y el endpoint existente de
login:

```http
POST http://localhost:8080/api/usuarios/login
Content-Type: application/json

{
  "email": "<SUPERUSUARIO_EMAIL>",
  "password": "<SUPERUSUARIO_PASSWORD>"
}
```

Guardar el token JWT de la respuesta sin escribirlo en logs ni en archivos del
repositorio. Las operaciones de métricas requieren el rol `SUPERUSUARIO`.

## 5. Disparar una carga incremental

La carga sin cuerpo procesa los partidos nuevos según la ventana incremental
definida por la feature:

```http
POST http://localhost:8080/api/metricas/sincronizacion
Authorization: Bearer <JWT>
X-Correlation-Id: manual-whoscored-<id>
```

La respuesta esperada es `202` con un `EjecucionMetricasResponse` que incluye el
identificador de ejecución, estado inicial y `correlationId`.

Para una carga desde una fecha inclusiva:

```http
POST http://localhost:8080/api/metricas/sincronizacion
Authorization: Bearer <JWT>
Content-Type: application/json
X-Correlation-Id: historica-whoscored-<id>

{
  "desde": "2026-08-01"
}
```

## 6. Qué ventana se abre

Con `headless: false`, el adapter abre una ventana de Chrome con el perfil
persistente configurado. Durante una carga puede navegar:

1. a una página de WhoScored usada como contexto para consultar el calendario
   mensual mediante `fetch`;
2. a las páginas `/Matches/{id}/Live` de los partidos que todavía no están
   cargados.

El adapter lee los datos desde JavaScript, no desde `page.content()`. Si la
página sigue navegando, reintenta la lectura. La ventana puede permanecer
abierta durante la pausa y el procesamiento de todos los partidos de la
carga.

## 7. Consultar el estado

Usar el `id` devuelto por el `202`:

```http
GET http://localhost:8080/api/metricas/sincronizacion/<ejecucionId>
Authorization: Bearer <JWT>
```

Repetir la consulta manualmente hasta que el estado sea `COMPLETADA`,
`PARCIAL` o `FALLIDA`. Revisar por liga:

- partidos cargados;
- partidos omitidos;
- jugadores vinculados;
- pendientes;
- error, si existe;
- `correlationId`;
- fechas de inicio y fin.

Si una segunda carga se intenta mientras la primera está `EN_CURSO`, debe
responder `409`. Si el backend se reinicia durante una carga, la ejecución
persistida debe pasar a `FALLIDA` al arrancar.

## 8. Consultar pendientes

```http
GET http://localhost:8080/api/metricas/pendientes?page=0&size=20
Authorization: Bearer <JWT>
```

Revisar los registros con `whoscoredId`, nombre, equipo, liga y
`detectadoEn`. Los homónimos o coincidencias ambiguas deben permanecer
pendientes y no asignarse arbitrariamente.

## 9. Evidencia y diagnóstico

Registrar para la verificación:

- configuración no sensible utilizada;
- id de ejecución y `correlationId`;
- estado final y resumen por liga;
- cantidad de jugadores vinculados y pendientes;
- partidos omitidos y errores;
- si Chrome llegó a las páginas de partidos.

No guardar cookies, credenciales, tokens ni el contenido del perfil persistente
en el repositorio. La medición del porcentaje de vinculación se realiza recién
en la primera carga real y se documenta en `research.md`.
