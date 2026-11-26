# Implementation Plan: Auditoría de web services

**Branch**: `entrega3-observabilidad` | **Date**: 2026-11-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-auditoria-web-services/spec.md`

## Summary

Un aspecto de Spring AOP (`AuditoriaWebServicesAspect`) envuelve con `@Around` todos los
métodos de los `@RestController` de `com.formula11.controller`. Por cada llamada arma una
`LineaAuditoria` con el usuario del `SecurityContext` (o `anónimo`), la operación, los
parámetros enmascarados por `EnmascaradorParametros`, la duración en ms y el resultado, y
la escribe en el logger `AUDITORIA`. Un `logback-spring.xml` nuevo manda ese logger a
`logs/auditoria.log` con rotación diaria y el correlation ID en el patrón, sin tocar la
salida de consola. No cambia ningún endpoint.

## Technical Context

- **Language/Version**: Java 21, Spring Boot 3.5.5.
- **Primary Dependencies**: nueva `spring-boot-starter-aop` (versión del parent). Logback y
  Jackson ya vienen con Spring Boot.
- **Storage**: ninguno. Archivo `logs/auditoria.log` (ya ignorado por el `.gitignore`).
- **Testing**: JUnit 5, Mockito, `ListAppender` de logback; Testcontainers (perfil `test`);
  MockMvc en `e2e` (perfil `e2e`).
- **Target Platform**: el mismo backend; sin infraestructura nueva.
- **Performance Goals**: el costo por llamada es serializar los argumentos y una escritura
  de una línea; no se espera impacto visible.
- **Constraints**: no cambiar respuestas ni códigos HTTP (SC-004); una falla al auditar no
  rompe el endpoint (FR-007).
- **Scale/Scope**: 11 controllers hoy, más los que sume A con el P2P.
- **Dependencias de otras ramas**: rama creada desde `dev` con el Paso 0 de C mergeado.
  Archivos compartidos: `pom.xml` (solo se agrega la dependencia de AOP) y
  `application.yml` (lo cambia solo B, según el acuerdo del grupo).
- **Migraciones**: ninguna.

## Constitution Check

| Principio | Cumplimiento |
|---|---|
| 1. Capas | El aspecto es transversal (`com.formula11.observabilidad`). No llama a services ni a repositories, y ningún controller depende de él. |
| 2. Modelo rico | No toca el modelo. |
| 3. Validaciones | Sin cambios: las validaciones siguen donde están. |
| 4. Seguridad | Las contraseñas y los tokens se enmascaran antes de escribir (FR-004). El usuario sale del `SecurityContext`. |
| 5. Observabilidad | Es el objetivo de la feature: log estructurado `clave=valor` y correlation ID en cada línea. |
| 6. Tests | Unit, integración y E2E, felices y borde. No se modifica ni borra ningún test existente. |
| 7. DoD | Logs estructurados y correlation ID cubiertos. No hay endpoints nuevos, así que Swagger y Postman no cambian. |
| 8. Idioma | Clases y mensajes en español (`LineaAuditoria`, `anónimo`); términos técnicos en inglés (`Aspect`). |
| 9. Clock | No aplica: el aspecto mide un intervalo con `System.nanoTime()` y la fecha la pone logback. No es un Service. |
| 10. Tokens | No aplica. |

**Resultado**: pasa, sin violaciones. Se vuelve a chequear después del diseño: igual.

## Project Structure

### Documentation (this feature)

```text
specs/015-auditoria-web-services/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/linea-auditoria.md
├── checklists/requirements.md
└── tasks.md              (lo crea /speckit-tasks)
```

### Source Code

```text
formula11/backend/
├── pom.xml                                            (modificado: spring-boot-starter-aop)
├── src/main/java/com/formula11/observabilidad/        (paquete transversal nuevo)
│   ├── AuditoriaWebServicesAspect.java                (nuevo, @Aspect @Component)
│   ├── EnmascaradorParametros.java                    (nuevo, sin estado)
│   └── LineaAuditoria.java                            (nuevo, record)
├── src/main/resources/
│   ├── logback-spring.xml                             (nuevo)
│   └── application.yml                                (modificado: app.auditoria.directorio)
└── src/test/
    ├── resources/application-test.yml                 (modificado: directorio en target/)
    ├── resources/application-e2e.yml                  (modificado: directorio en target/)
    └── java/com/formula11/
        ├── unit/observabilidad/
        │   ├── EnmascaradorParametrosTest.java        (nuevo)
        │   ├── LineaAuditoriaTest.java                (nuevo)
        │   └── AuditoriaWebServicesAspectTest.java    (nuevo)
        ├── integration/observabilidad/
        │   └── AuditoriaWebServicesIntegrationTest.java (nuevo)
        └── e2e/observabilidad/
            └── AuditoriaWebServicesE2ETest.java       (nuevo)
```

**Structure Decision**: paquete transversal nuevo `observabilidad` (research R8), que
después reciben el health indicator y las métricas de la feature 016. Hay que avisar a C
para que `ArquitecturaTest` lo trate como `config`, `exception` y `security`.

## Architecture and Design

### Flujo de una llamada

```text
CorrelationIdFilter (MDC correlationId)
  → JwtAuthenticationFilter (SecurityContext)
    → DispatcherServlet → proxy del controller
      → AuditoriaWebServicesAspect.auditar()
          inicio = nanoTime
          proceed()  ──► método del controller
          resultado = OK | ERROR(Excepcion)   (la excepción se vuelve a lanzar tal cual)
          LineaAuditoria(usuario, operacion, parametros, duracionMs, resultado)
          log AUDITORIA.info(linea.formatear())
      → GlobalExceptionHandler (si hubo error, sin cambios)
```

### Decisiones clave (detalle en [research.md](research.md))

- Pointcut `within(com.formula11.controller..*) && @within(RestController)` (R1).
- No se auditan los 401, 403 y 400 de validación del cuerpo que se cortan antes del
  controller (R2).
- Enmascarado por nombre de campo sobre el árbol JSON del argumento (R4).
- Formato `clave=valor`; fecha y correlation ID desde el patrón de logback (R5, R6).
- Todo lo que prepara la línea va en `try/catch`; nunca cambia el resultado del endpoint (R7).

### Configuración

`application.yml`:

```yaml
app:
  auditoria:
    directorio: ${APP_AUDITORIA_DIRECTORIO:logs}
```

Perfiles `test` y `e2e`: `app.auditoria.directorio: target/auditoria-test` y
`target/auditoria-e2e`, para que los tests lean el archivo sin ensuciar el repo.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Proxies CGLIB sobre los controllers cambian algo de los tests `@WebMvcTest` | `@WebMvcTest` no carga `@Component` de aspectos; se corre toda la suite (`mvn verify`). |
| El archivo de tests lo comparten varias clases en la misma JVM | Cada test busca su línea por un correlation ID o un email únicos. |
| `ArquitecturaTest` de C marca el paquete nuevo | Coordinar con C antes de mergear (R8). |

## Release notes (para el PR)

- Auditoría de web services: cada llamada a la API deja una línea en
  `logs/auditoria.log` (usuario, operación, parámetros enmascarados, duración, resultado y
  correlation ID). Probar con un login y `Select-String -Path logs\auditoria.log -Pattern "<correlationId>"`.

## Complexity Tracking

Sin violaciones de la constitución.
