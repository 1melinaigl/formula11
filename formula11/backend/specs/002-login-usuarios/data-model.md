# Data Model: Login de usuarios

## Entidades existentes reutilizadas

### Usuario

Representa una cuenta registrada que puede autenticarse.

| Campo | Tipo lógico | Reglas relevantes |
|---|---|---|
| `id` | Identificador | Generado por la persistencia; se devuelve en login exitoso. |
| `nombre` | Texto | Obligatorio; se devuelve en login exitoso. |
| `email` | Texto | Obligatorio, formato válido, normalizado con trim y minúsculas; búsqueda exacta sobre el valor normalizado. |
| `passwordHash` | Texto protegido | Obligatorio; nunca se devuelve; se verifica con BCrypt. |
| `fechaRegistro` | Fecha/hora | Existente en el modelo; no forma parte de `LoginResponse`. |

La tabla `usuarios` y su índice `ux_usuarios_email_normalizado` ya existen en `src/main/resources/db/migration/V1__crear_tablas_iniciales.sql`; no se requiere una migración nueva.

## Nuevos DTOs

### LoginRequest

Record de entrada del endpoint:

| Campo | Tipo | Validación |
|---|---|---|
| `email` | String | `@NotBlank`, máximo 254 caracteres y formato de email válido; el valor se normaliza después de la validación de forma. |
| `password` | String | `@NotBlank`, entre 8 y 72 caracteres; se usa solo para comparar contra `passwordHash`. |

### LoginResponse

Record de salida del login exitoso:

| Campo | Tipo | Fuente |
|---|---|---|
| `id` | Long | `Usuario.id` |
| `nombre` | String | `Usuario.nombre` |
| `email` | String | `Usuario.email` ya normalizado |
| `token` | String | `JwtService.generarToken` |

No contiene contraseña, hash, fecha de registro ni información de si una búsqueda previa falló.

## Flujo de estados

1. Request HTTP entra al endpoint público y pasa validación de forma.
2. `UsuarioService` normaliza el email y busca el usuario.
3. Si no existe, lanza `CredencialesInvalidasException`.
4. Si existe, BCrypt compara la contraseña con `passwordHash`.
5. Si no coincide, lanza la misma excepción.
6. Si coincide, `JwtService` emite el token y se construye `LoginResponse`.
7. El handler global convierte fallos de credenciales en `401 ErrorResponse`; los fallos de validación permanecen en `400 ErrorResponse`.
