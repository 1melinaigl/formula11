# Quickstart: Validar el mercado de tokens

## Prerrequisitos

- Java 21 y Maven disponibles.
- Docker disponible para los tests de integración con PostgreSQL.
- Ejecutar desde `backend/`.
- Aplicar el perfil `local` para desarrollo. Ese perfil contiene credenciales de desarrollo del superusuario.
- Para otros perfiles, definir `SUPERUSUARIO_EMAIL` y `SUPERUSUARIO_PASSWORD`.

## Validación automatizada

```powershell
cd backend
.\mvnw test
```

Los tests deben cubrir:

- Unitarios de `MercadoService` con `Clock.fixed`: compra, venta, promedio ponderado, saldo/stock/tenencia insuficientes y precio fijo.
- Integración de repositories y migraciones con PostgreSQL/Testcontainers.
- E2E MockMvc de consulta de mercado, compra y venta, incluyendo `400`, `401`, `403` (superusuario), `404`, `409` y `422`.
- Inicialización idempotente del superusuario y propagación de rol en login/JWT.
- Protección append-only de auditoría y atomicidad ante una excepción.

## Flujo manual

1. Levantar el backend con el perfil local:

   ```powershell
   .\mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. Registrar un usuario mediante `POST /api/usuarios/registro` y conservar el JWT.
3. Consultar `GET /api/jugadores/{jugadorId}/mercado` con `Authorization: Bearer <token>`. La respuesta debe informar `tokensEmitidos: 100`, el stock disponible y `precioVigente: 1`.
4. Ejecutar `POST /api/ordenes/compra` con:

   ```json
   {
     "jugadorId": 1,
     "cantidad": 2
   }
   ```

   Debe responder `201`, descontar 2 créditos del saldo del usuario, reducir en 2 la tenencia del superusuario y crear transacción/auditoría.
5. Ejecutar `POST /api/ordenes/venta` con el mismo jugador y cantidad. Debe responder `201`, acreditar 2 créditos y devolver los tokens al superusuario.
6. Repetir los casos de error y comprobar que no quedan cambios parciales:
   - cantidad inválida: `400`;
   - jugador inexistente: `404`;
   - stock insuficiente en compra: `409`;
   - saldo insuficiente o tenencia insuficiente: `422`;
   - JWT ausente/inválido: `401`;
   - orden enviada con el JWT del superusuario: `403`.

## Contrato y documentación

La fuente contractual es `../005-contratos-entrega2/contracts/openapi.yaml`. La extracción de endpoints relevantes está documentada en `contracts/openapi-market.md`.
