# Implementation Plan: Portfolio e historial de operaciones

**Branch**: `009-portfolio-historial` | **Date**: 2026-10-25 | **Spec**: [spec.md](./spec.md)

## Summary

Implementar las consultas de portfolio actual, evolución histórica e historial
paginado sobre el backend Spring Boot existente. La feature reutilizará `Cuenta`,
`Tenencia`, `Transaccion`, `Jugador`, `Usuario` y sus repositorios de la 007; no
creará tablas. El dominio quedará en `PortfolioService`, el controller solo
resolverá el principal JWT, traducirá parámetros HTTP y mapeará resultados a DTOs.
La valuación histórica dependerá de `CotizacionHistoricaPort`, con un adapter
temporal de precio fijo hasta que se incorpore la implementación de la 008.

## Technical Context

**Language/Version**: Java 21  
**Framework**: Spring Boot 3.5.5, Spring Web, Spring Data JPA, Spring Security/JJWT  
**Storage**: PostgreSQL existente; sin migraciones nuevas salvo un índice V26 si el
plan de consulta demuestra que el índice de la 007 no alcanza  
**Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc y Testcontainers PostgreSQL  
**Runtime**: Servicio HTTP stateless con perfiles `local`, `test` y producción  
**Time**: `Clock` inyectado; `fechaValuacion = LocalDate.now(clock)` en UTC y
fechas de transacción convertidas a `LocalDate` en UTC
**Rounding**: importes a escala 4 y porcentajes a escala 2, siempre `HALF_UP`

## Constitution Check

- **Arquitectura en capas**: PASS. `PortfolioController` traduce HTTP; `PortfolioService`
  orquesta repositories, modelos y puertos; `adapter` implementa el puerto histórico.
- **Separación DTO/dominio**: PASS. Los tres métodos del service devuelven resultados
  de dominio inmutables; el controller construye `PortfolioResponse`,
  `EvolucionPortfolioResponse` y `TransaccionesPageResponse`.
- **Validación por nivel**: PASS. Spring valida tipos y rangos HTTP; el service
  valida propietario, superusuario, fechas y existencia del usuario propio; el
  repositorio aplica filtros y paginación.
- **Seguridad**: PASS. La comparación del id del path con
  `usuarioService.obtenerIdPorEmail(authentication.getName())` ocurre antes de
  buscar el usuario del path. La excepción de acceso se traduce a `403`.
- **Tiempo reproducible**: PASS. Portfolio y evolución usan el `Clock`; la
  reconstrucción histórica usa `Instant` convertido con `ZoneOffset.UTC`.
- **Calidad**: PASS. Se agregarán unit tests del service, integration tests con
  PostgreSQL real y E2E MockMvc para los tres endpoints, sin borrar tests existentes.
- **Documentación**: PASS. Se verificará que OpenAPI y la colección Postman ya
  reflejen los endpoints; esta feature no modifica `openapi.yaml` ni agrega
  esquemas al contrato.

## Design

### Flujo de autorización

`PortfolioController` obtiene el email del `Authentication`, llama a
`usuarioService.obtenerIdPorEmail` y obtiene el rol desde la autoridad JWT.
Pasa ambos datos al guard privado común de `PortfolioService` junto con
`{usuarioId}`. Si no coincide el id, o la autoridad es `ROLE_SUPERUSUARIO`,
`PortfolioService` lanza una excepción de dominio de acceso prohibido antes de
cualquier búsqueda del usuario indicado, evitando filtrar si existe. Los tres
métodos públicos (`portfolio`, `evolucion`, `historial`) ejecutan ese guard como
primer paso; el usuario propio se resuelve después y su ausencia produce `404`.

### Portfolio actual

`PortfolioService.portfolio(usuarioId)` obtiene la cuenta y las tenencias del
usuario. Descarta cantidades cero, pide todas las cotizaciones vigentes con
`CotizacionVigentePort`, y calcula por posición y totales:

- `invertido = cantidad * precioPromedio`
- `valorActual = cantidad * precioActual`
- `ganancia = valorActual - invertido`
- porcentaje cero cuando el invertido es cero; en otro caso `ganancia / invertido * 100`

Todos los importes se normalizan a escala 4 y porcentajes a escala 2.
`fechaValuacion` será `LocalDate.now(clock)` usando UTC.

### Evolución

`PortfolioService.evolucion(usuarioId, desde, hasta)` resuelve defaults con el
`Clock` y la primera transacción del usuario, valida `desde <= hasta`, y obtiene
las fechas exclusivamente mediante `CotizacionHistoricaPort.fechasCotizacion`.
Las transacciones se consultan con filtros en la base de datos, se cargan en orden
cronológico y se recorren una sola vez:
una compra suma cantidad y recalcula precio promedio ponderado; una venta resta
cantidad sin modificarlo. Para cada fecha se piden todos los precios mediante
`preciosA`, usando los ids de jugadores presentes y la fecha de cotización.
La conversión de cada `Transaccion.fecha` a fecha calendario usa UTC y se incluyen
las operaciones cuya fecha UTC sea menor o igual al punto. Se devuelve un punto
por cada fecha recibida, incluso si sus tenencias quedan vacías. Tanto las fechas
como los precios usan la estrategia activa al momento de la consulta, nunca una
estrategia registrada en la transacción.

`CotizacionHistoricaFijaAdapter` devuelve lunes semanales dentro del intervalo,
sin duplicados, y precio `1.0000` para cada jugador pedido. Será un bean activo
en todos los perfiles y se eliminará al integrar la implementación de la 008.

### Historial

`PortfolioService.historial(usuarioId, page, size, tipo, jugadorId)` delega en
`TransaccionRepository` con usuario obligatorio, filtros opcionales combinados
con AND y orden `fecha DESC, id DESC`. El `jugadorId` no se valida contra el
catálogo: si no existe, la consulta devuelve `200` con contenido vacío. El
controller valida `jugadorId` con `@Positive`; cero, negativos y valores no
numéricos responden `400`.
El resultado de dominio conservará metadatos de `Page` y transacciones para que el
controller reutilice `OperacionResponse`. Como `Transaccion` no persiste el saldo
posterior, el service consultará `AuditoriaMercadoRepository` una sola vez con los
ids de las transacciones de la página. De cada auditoría leerá
`despues.saldoUsuario`, que la 007 persiste en JSONB, y construirá un mapa
`transaccionId -> saldoPosterior` para convertir la página mediante
`OperacionResponse.from(Transaccion, BigDecimal saldoPosterior)`. Para obtener la
operación relacionada, `AuditoriaMercado` expondrá `getOperacion()`. Se mantiene
`OperacionResponse.from(ResultadoOperacion)`. El repositorio conservará su carácter
append-only: solo se agregará un método de lectura por ids, sin métodos de update ni
delete.

### Demo local

Agregar `DemoPortfolioInitializer` como `ApplicationRunner`, con
`@Profile("local")` y `@Order` posterior a `SuperusuarioInitializer`. Si los
usuarios demo identificados por los emails configurados en `mercado.demo.usuarios`
ya existen, no hará nada. En caso contrario registrará esos cuatro usuarios con
`UsuarioService`, seleccionará cinco jugadores existentes y ejecutará cinco compras
por usuario mediante `MercadoService.comprar(usuarioId, jugadorId, cantidad,
Clock)`. La sobrecarga delegará en la implementación actual usando el reloj
recibido, preservando validaciones, auditoría y transacción.
`mercado.demo.usuarios` y `mercado.demo.fecha-inicio` serán configurables en
`application-local.yml`, con cuatro emails por defecto.

## Project Structure

### Documentation

```text
backend/specs/009-portfolio-historial/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
└── quickstart.md
```

### Main source changes

```text
backend/src/main/java/com/formula11/
├── adapter/
│   └── CotizacionHistoricaFijaAdapter.java
├── config/
│   └── MercadoProperties.java                 # fecha y emails demo si se centraliza allí
├── controller/
│   └── PortfolioController.java
├── dto/
│   ├── PortfolioResponse.java
│   ├── PosicionPortfolioResponse.java
│   └── EvolucionPortfolioResponse.java       # incluye punto y página si aplica
├── exception/
│   ├── AccesoPortfolioDenegadoException.java
│   └── GlobalExceptionHandler.java            # mapeo 403
├── persistence/
│   ├── TransaccionRepository.java             # queries de lectura
│   └── TenenciaRepository.java                # consulta de tenencias
└── service/
    ├── CotizacionHistoricaPort.java
    ├── PortfolioService.java
    ├── PortfolioResultados.java               # resultados de dominio
    ├── DemoPortfolioInitializer.java
    └── MercadoService.java                    # overload con Clock
```

Los nombres concretos de records auxiliares pueden ajustarse a las convenciones
existentes, pero no se deben introducir DTOs en la API del service.

### Tests

```text
backend/src/test/java/com/formula11/
├── unit/service/PortfolioServiceTest.java
├── unit/adapter/CotizacionHistoricaFijaAdapterTest.java
├── integration/PortfolioPersistenceIntegrationTest.java
└── e2e/PortfolioE2ETest.java
```

## Implementation Phases

1. **Foundation**: definir el puerto histórico, adapter fijo, resultados de dominio,
   excepción de autorización y configuración de la fecha demo.
2. **Persistence**: agregar consultas de tenencias/transacciones y verificar el plan
   de ejecución; crear V26 únicamente si se observa una necesidad real.
3. **Service**: implementar portfolio, evolución, historial y la sobrecarga de compra
   con `Clock`, incluyendo escala, UTC, defaults y validaciones.
4. **HTTP**: agregar controller, DTOs, mapeos y `GlobalExceptionHandler`; documentar
   los endpoints en OpenAPI/Swagger y Postman.
5. **Demo**: agregar el initializer local idempotente, configurable por emails, y
   comprobar el orden respecto de `SuperusuarioInitializer`.
6. **Verification**: ejecutar unit, integration y E2E; levantar perfil local dos veces
   y comprobar que no duplica usuarios ni operaciones.

## Complexity Tracking

No se prevén violaciones constitucionales ni nuevas dependencias. La única decisión
condicional es V26: solo se crea si el análisis del plan de ejecución de PostgreSQL
demuestra que el índice `(usuario_id, fecha DESC)` existente no cubre los filtros
requeridos.
