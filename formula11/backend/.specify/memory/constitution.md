# Constitution - Proyecto Valoración de Mercado de Jugadores

## 1. Arquitectura en capas extendida

La arquitectura utilizará una división en capas con Controllers, Services, Repositories y se agrega explícitamente una capa de Adapters para manejar las APIs externas.

- Controller solo habla con Service.
- Service orquesta entre Model, Persistence y Adapters. El Scheduler no es orquestado por el Service: el Scheduler es quien dispara al Service (llama a los mismos métodos que expone para el recálculo manual). No hay lógica de negocio en el Scheduler, solo la programación temporal del trigger.
- Model no conoce ni interactúa con ninguna otra capa.
- Persistence y Adapters conocen al Model, pero no hacen llamadas sobre él.
- La caché (Redis o in-memory) es un requisito **obligatorio** de infraestructura, no opcional, y se maneja en las capas de Adapters/Persistence. Su propósito es permitir que el sistema siga funcionando con datos locales si WhoScored o Football-Data.org fallan.

## 2. Modelo rico en el Dominio Financiero y Deportivo

La lógica de negocio vive en los objetos de modelo (Jugador, Cotizacion, Estrategia, Usuario, Portfolio, Transaccion).

- El cálculo del score basado en las métricas (goles, asistencias, tarjetas, minutos jugados, tiros, pases, intercepciones, posición, rating, etc.) y su conversión a un precio monetario deben estar encapsulados en el dominio.
- El sistema debe soportar **al menos dos estrategias** de cotización configurables, cada una con un peso configurable por métrica (no alcanza con que la estrategia en sí sea seleccionable; cada métrica dentro de ella debe tener su ponderación ajustable).
- Cada cotización debe dejar traza histórica de qué estrategia (y qué versión de esa estrategia) se usó para calcularla.
- La lógica dura del mercado (emisión inicial de 100 tokens por jugador a 1 crédito, el rol del superusuario como dueño original, la validación de que las compras iniciales de usuarios se resuelven contra el superusuario) es responsabilidad de validación interna del modelo.
- El recálculo automático de la posición (ganancia/pérdida) del portfolio de un usuario, cada vez que cambia la cotización de un jugador que posee, es una operación de dominio: la dispara el Service inmediatamente después de persistir la nueva cotización, pero el cálculo en sí vive en el modelo `Portfolio`/`Posicion`, no en el Service.

## 3. Cada validación en su nivel

- **DTO de Request:** forma, tipos, trimming y sanitización del input. Fundamental para blindar los endpoints de mercado como `POST /orders/buy` y `POST /orders/sell`.
- **Service:** valida que lo pedido exista y la acción sea posible de orquestar (los ids resuelven, hay disponibilidad de tokens para comprar, el usuario posee el saldo necesario de tokens para vender).
- **Model:** invariantes del dominio. Lanza excepciones propias (ej. que una transacción genere un estado inválido o no cumpla los requisitos para ser registrada).

## 4. Seguridad

- Todos los endpoints, salvo el registro de usuario, requieren autenticación.
- **Supuesto documentado:** se interpreta que la "ApiKEY" mencionada en la planilla de entregas y el JWT son la misma credencial — el token JWT emitido al momento del registro/login de un usuario, no un mecanismo de autenticación separado tipo M2M. Si en una corrección se aclara lo contrario, este punto se revisa.
- Estricta validación de todas las entradas de datos (cubierto también por el punto 3, capa de DTO).

## 5. Observabilidad

- Logs estructurados para facilitar el análisis.
- Correlation ID para garantizar trazabilidad de solicitudes entre servicios.
- Health checks y exposición de métricas clave (latencia, tasa de error).

## 6. Tests y Calidad

- **Unitarios del dominio:** sin el framework y sin base de datos. Deben probar exhaustivamente el cálculo matemático de las estrategias de cotización y las variaciones de ganancia/pérdida dentro del portfolio del usuario.
- **Integración:** Services y Repositories contra PostgreSQL real levantado con Testcontainers.
- **End to end (E2E) con Supertest:** validando flujos completos de lectura (ej. `GET /players/:id/quotes`) y transaccionales. Solo en su propio paquete. Nunca dentro de un test de service.
- Siempre se deben contemplar casos felices y casos borde (por ejemplo, el comportamiento del sistema cuando el scraper de WhoScored o la API de Football-Data.org fallan).
- No se modifica ni se borra un test existente, en ninguna fase del flujo, sin pedir permiso y recibir un "sí" explícito.

## 7. Definición de terminado y entregable (DoD)

Un requerimiento está terminado cuando:

- Tiene tests unitarios y de integración, felices y borde, y pasan.
- La aplicación compila y levanta con la configuración local.
- Se implementaron logs estructurados, Correlation ID, y auditoría inmutable de transacciones (si el requerimiento operó sobre tokens).
- La colección de Postman y la documentación OpenAPI (Swagger) quedaron actualizadas con los endpoints nuevos.

## 8. Idioma y Nomenclatura

- Documentos y mensajes de error en español.
- Nombres del dominio en español, sin acentos ni ñ en identificadores (ej. `Cotizacion`, `Transaccion`, `Jugador`, `Portfolio`).
- Términos técnicos y normativos en inglés (ej. `Scraper`, `Job`, `Scheduler`, `Cache`, `Adapters`).

---

**Version**: 2.0.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-09-08
