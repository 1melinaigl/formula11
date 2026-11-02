# Research: Mercado de tokens

## Decision: Reutilizar el monolito Spring Boot y los paquetes existentes

**Rationale**: El backend ya usa Controllers, Services, Repositories, Model, DTO, Security y Flyway. Agregar módulos o una API paralela duplicaría autenticación, manejo de errores y configuración.

**Alternatives considered**: Crear un módulo de mercado separado o un servicio independiente. Se descarta porque el contrato 005, la autenticación JWT y la base PostgreSQL son compartidos por las features de la Entrega 2.

## Decision: `MercadoService` como única entrada de negocio

**Rationale**: Compra y venta deben poder invocarse desde HTTP y desde la compra en un click sin duplicar validaciones ni transferencias. Los controllers solo convertirán DTOs y traducirán el resultado.

**Alternatives considered**: Implementar la lógica en cada controller o crear un service distinto por endpoint. Se descarta porque generaría divergencia entre flujos y violaría la sección 10 de la constitución.

## Decision: Bloqueo pesimista sobre cuentas y tenencias

**Rationale**: Una orden modifica simultáneamente saldo, tenencia del usuario y tenencia del superusuario. `SELECT FOR UPDATE` en el orden estable cuenta → tenencias reduce doble gasto, sobreventa y carreras al actualizar el mismo jugador.

**Alternatives considered**: Solo optimismo con reintentos o sincronización en memoria. Se descartan porque no protegen correctamente múltiples instancias ni la base PostgreSQL compartida.

## Decision: Superusuario creado por `ApplicationRunner`, idempotente

**Rationale**: Las credenciales dependen del perfil/entorno y no deben estar en SQL. El runner busca por email, crea la fila de `usuarios` con rol `SUPERUSUARIO` si no existe (sin cuenta de créditos) y asegura, con un insert idempotente, la tenencia de 100 tokens del superusuario para cada jugador que no la tenga. Esto no puede ir en una migración porque Flyway corre antes del runner, cuando el superusuario todavía no existe. En `local` y `test` se usan defaults de desarrollo; fuera de esos perfiles las variables son obligatorias.

**Alternatives considered**: Insertar credenciales en Flyway o crear credenciales aleatorias. Se descartan por no poder hashear secretos de entorno en migraciones y por hacer impredecible el acceso administrativo.

## Decision: `CotizacionVigentePort` con adapter fijo

**Rationale**: El mercado depende de una abstracción que luego podrá conectarse a la estrategia A. `CotizacionFijaAdapter` devuelve precio 1, estrategia/version/fecha de cotización nulas, exactamente como el contrato 005.

**Alternatives considered**: Acoplar `MercadoService` a un literal o consultar una tabla de cotizaciones aún inexistente. Se descartan porque dificultan reemplazar la estrategia y contradicen el alcance temporal.

## Decision: `Clock` como bean inyectado

**Rationale**: Permite reproducir fechas de transacciones/auditorías y soportar la demo con fecha fija. Los services no invocarán directamente `Instant.now()` o `LocalDate.now()`.

**Alternatives considered**: Usar la hora del sistema en cada operación. Se descarta por incumplir la constitución y dificultar tests deterministas.

## Decision: Auditoría append-only mediante entidad/repository sin update/delete

**Rationale**: Cada operación insertará autor, estados antes/después y timestamp en la misma transacción. La API de persistencia no expondrá mutadores ni operaciones de borrado.

**Alternatives considered**: Registrar solo logs o permitir editar un registro de auditoría. Se descartan porque los logs no son el historial persistente requerido y editarlo rompe la inmutabilidad.

## Decision: Flyway V20-V25 con `out-of-order` solo en local/test

**Rationale**: La feature debe convivir con migraciones existentes y el repositorio reserva el rango V20-V29 para B; esta feature usa V20-V25, cada migración con sus constraints e índices, y deja V26-V29 libres. `spring.flyway.out-of-order=true` permite levantar bases locales/test con versiones intermedias ya aplicadas, sin habilitar ese comportamiento implícitamente en otros perfiles.

**Alternatives considered**: Modificar migraciones V1-V3 o usar `ddl-auto=update`. Se descartan porque romperían historial de Flyway y la validación de esquema existente.

## Decision: El superusuario no tiene cuenta, pero sí tenencias

**Rationale**: El enunciado define al superusuario como dueño inicial de los tokens, no de créditos. Sus tokens se guardan en `tenencias` porque son el stock: sin ellos no se valida la disponibilidad al comprar ni se calcula `tokensDisponibles`. Sus créditos no existen, así que no tiene fila en `cuentas` y la regla `saldo >= 0` aplica a todas las cuentas sin excepciones. La auditoría registra el saldo del usuario y los tokens de ambos lados.

**Alternatives considered**: Darle una cuenta con saldo "ilimitado". Se descarta porque obliga a excepciones sobre la restricción `saldo >= 0` y registra un saldo sin significado.

## Decision: `403` para órdenes del superusuario

**Rationale**: Si el superusuario compra o vende, opera contra sí mismo. `MercadoService` lo rechaza con una excepción de dominio (así también se cubre la invocación interna) que el `GlobalExceptionHandler` traduce a `403` con una respuesta propia, `ErrorSuperusuarioNoOpera` (`ErrorProhibido` describe el acceso a recursos de otro usuario). El contrato 005 todavía no lo contempla: B lo agrega en el PR de la rama `005-contrato-403-superusuario`, junto con el `403` del superusuario en el portfolio (009).

**Alternatives considered**: `422`. Se descarta porque no es una regla de negocio incumplida por los datos de la orden, sino un actor que no tiene permitido operar.
