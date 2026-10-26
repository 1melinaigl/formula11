# Quickstart: Portfolio e historial de operaciones

## Prerrequisitos

- Java 21
- Maven disponible mediante `mvnw` o `mvn`
- Docker disponible para los tests de integración con Testcontainers
- PostgreSQL local para el perfil `local`

## Verificación de desarrollo

Desde `backend`:

```powershell
.\mvnw.cmd test
```

Para ejecutar solo la cobertura de la feature:

```powershell
.\mvnw.cmd -Dtest=PortfolioServiceTest,CotizacionHistoricaFijaAdapterTest test
```

Los tests de integración y E2E deben ejecutarse con Docker disponible:

```powershell
.\mvnw.cmd -Dtest=PortfolioPersistenceIntegrationTest,PortfolioE2ETest test
```

## Ejecución local

Configurar PostgreSQL y arrancar con:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

El perfil local ejecuta `SuperusuarioInitializer` y luego
`DemoPortfolioInitializer`. El segundo es idempotente: un segundo arranque no
crea usuarios ni operaciones adicionales.

## Endpoints

Con un JWT de usuario:

```text
GET /api/usuarios/{usuarioId}/portfolio
GET /api/usuarios/{usuarioId}/portfolio/evolucion?desde=2026-01-01&hasta=2026-03-01
GET /api/usuarios/{usuarioId}/transacciones?page=0&size=20&tipo=COMPRA&jugadorId=1
```

Casos mínimos a verificar:

- `200` para las tres consultas propias;
- `400` para fechas inválidas, `desde > hasta` y parámetros de página inválidos;
- `401` sin JWT o con JWT inválido;
- `403` para otro usuario y para el superusuario;
- `200` con página vacía para un `jugadorId` inexistente en el historial.
