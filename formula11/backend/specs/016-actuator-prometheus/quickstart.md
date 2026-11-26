# Quickstart: Actuator y Prometheus

Guía para comprobar la feature de punta a punta. Los contratos están en
[contracts/actuator.md](contracts/actuator.md) y
[contracts/metricas-negocio.md](contracts/metricas-negocio.md).

## Requisitos

- Docker corriendo (Testcontainers, y Postgres local o descartable).
- Backend con el perfil `local` en `http://localhost:8080`.
- Usuarios del perfil local: superusuario `superusuario@formula11.com` y demo
  `demo1@formula11.com` (contraseñas en `application-local.yml`).

## 1. Tests

```powershell
mvn verify
```

Esperado: todos los tests en verde, incluidos los nuevos de `unit/observabilidad`,
`integration/observabilidad` y `e2e/observabilidad`, sin cambios en los existentes.

## 2. Health público

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Esperado: `status` igual a `UP` y ningún otro campo.

## 3. Seguridad

```powershell
$super = (Invoke-RestMethod -Method Post http://localhost:8080/api/usuarios/login -ContentType application/json -Body '{"email":"superusuario@formula11.com","password":"<contraseña>"}').token
$demo  = (Invoke-RestMethod -Method Post http://localhost:8080/api/usuarios/login -ContentType application/json -Body '{"email":"demo1@formula11.com","password":"<contraseña>"}').token
```

```powershell
Invoke-WebRequest http://localhost:8080/actuator/info -SkipHttpErrorCheck | Select-Object StatusCode
```

```powershell
Invoke-WebRequest http://localhost:8080/actuator/info -Headers @{Authorization="Bearer $demo"} -SkipHttpErrorCheck | Select-Object StatusCode
```

```powershell
Invoke-RestMethod http://localhost:8080/actuator/info -Headers @{Authorization="Bearer $super"}
```

Esperado: 401, 403 y después `app.nombre` y `app.version`. `-SkipHttpErrorCheck` es de
PowerShell 7; en Windows PowerShell 5.1 el 401 o el 403 aparecen como error con el código.

## 4. Health con detalle y carga fallida

```powershell
(Invoke-RestMethod http://localhost:8080/actuator/health -Headers @{Authorization="Bearer $super"}).components.cargaMetricas
```

Esperado: estado y detalle de la última carga. Para ver el `DOWN`, se puede forzar una carga
que falle (por ejemplo, WhoScored en modo `archivos` con una carpeta vacía) o marcar la
última ejecución como `FALLIDA` en un Postgres descartable. Después, el health público da 503
con `status` en `DOWN`.

## 5. Métricas de negocio

1. Comprar un token con el usuario demo: `POST /api/ordenes/compra` con `{"jugadorId": <id>, "cantidad": 1}`.
2. Leer las métricas:

```powershell
(Invoke-WebRequest http://localhost:8080/actuator/prometheus -Headers @{Authorization="Bearer $super"}).Content -split "`n" | Select-String formula11_
```

Esperado: `formula11_mercado_operaciones_total{tipo="compra"}` sube en 1; `venta` y `p2p`
aparecen aunque estén en 0. Después de `POST /api/cotizaciones/recalcular` (superusuario),
aparece `formula11_cotizaciones_recalculo_seconds_count{resultado="ok"}`.

## 6. Nivel de log en caliente

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/actuator/loggers/AUDITORIA -Headers @{Authorization="Bearer $super"} -ContentType application/json -Body '{"configuredLevel":"WARN"}'
```

Esperado: las llamadas siguientes no agregan líneas a `logs/auditoria.log`. Se vuelve con
`{"configuredLevel":null}`.

## 7. Prometheus local (opcional)

```powershell
Set-Content -NoNewline -Encoding ascii docker\prometheus\token $super
```

```powershell
docker compose -f docker\prometheus\docker-compose.yml up -d
```

Abrir `http://localhost:9090/targets`: el objetivo `formula11` figura `UP`. En la consulta,
`formula11_mercado_operaciones_total` muestra los tres tipos. Cuando el token vence (una
hora), se repite el `Set-Content` con un token nuevo; Prometheus lo toma en el scrape siguiente.
Se apaga con `docker compose -f docker\prometheus\docker-compose.yml down`.
