<!--
Sync Impact Report
- Version change: none -> 1.0.0
- Modified principles: template placeholders -> five frontend governance principles
- Added sections: Additional Constraints; Development Workflow
- Removed sections: none
- Follow-up TODOs: RATIFICATION_DATE requires confirmation of the original adoption date.
-->

# Formula11 Frontend Constitution

## Core Principles

### I. Fidelidad visual verificable
`design/formula11-mockup-retro-v3.html` es la fuente de verdad del look and feel.
La implementación MUST conservar su paleta, tipografías Bebas Neue y Barlow, paneles,
botones inclinados, scanlines, cancha de fondo y corte responsive en 760px. Toda
desviación visual MUST quedar justificada en el plan de la feature y validada antes de
considerarse terminada.

### II. Contratos del backend como autoridad
Los endpoints, campos, estados y errores MUST provenir de
`backend/specs/001-registro-catalogo-jugadores/contracts/openapi.yaml` y
`backend/specs/002-login-usuarios/contracts/openapi.yaml`. El frontend MUST NOT inventar
endpoints, campos ni datos. Una capacidad no ofrecida por el backend MUST mostrarse como
"Próximamente" o registrarse explícitamente como dependencia.

### III. Capas y límites explícitos
La dependencia MUST fluir como páginas, componentes, hooks/estado y cliente de API.
Los componentes MUST NOT ejecutar `fetch` ni conocer detalles de transporte; toda llamada
al backend MUST pasar por el cliente de API y sus hooks o servicios de estado. Las
interfaces públicas entre capas MUST permanecer pequeñas y comprobables.

### IV. Validación, seguridad y trazabilidad
Los formularios MUST aplicar las mismas reglas de forma que los DTO: nombre de 1 a 100
caracteres, email válido de hasta 254 caracteres y password de 8 a 72 caracteres,
incluyendo la normalización de espacios externos del email cuando el contrato la exige.
El backend sigue siendo la autoridad final. Los errores MUST mostrarse en español junto
con su `correlationId`. El JWT MUST almacenarse solo según la estrategia de sesión
aprobada, nunca mostrarse en pantalla ni escribirse en logs; cualquier `401` en un
endpoint protegido MUST limpiar la sesión.

### V. Calidad accesible y comprobable
Las pruebas MUST usar Vitest, React Testing Library y MSW para cubrir casos felices y
bordes, incluyendo `400`, `401`, `409` y catálogo vacío. No se puede modificar ni borrar
un test existente sin permiso explícito. La interfaz MUST ser navegable por teclado con
flechas, Enter y Esc donde corresponda, usar labels asociados y mantener foco visible.
Un cambio está terminado solo cuando `npm run build`, lint y la suite de tests pasan y la
pantalla coincide con el mockup.

## Additional Constraints

La UI, la documentación y los errores MUST estar en español. Los identificadores del
dominio MUST estar en español y no contener acentos ni `ñ`; los términos técnicos MUST
permanecer en inglés cuando sean parte del stack o del contrato. React y Vite son la
base del frontend. Los contratos OpenAPI son la única fuente autorizada para integrar
registro, login y catálogo. El catálogo vacío es un estado válido y MUST tener una
representación explícita.

## Development Workflow

Cada feature MUST comenzar identificando el contrato backend y la referencia visual que
la gobiernan. El plan MUST registrar las desviaciones visuales y las dependencias de
capacidades ausentes. La implementación MUST mantener las capas definidas y añadir o
actualizar pruebas MSW para cada contrato afectado. La revisión MUST comprobar
accesibilidad, ausencia de JWT en UI y logs, tratamiento de `401`, mensajes con
`correlationId` y los comandos de la definición de terminado.

## Governance
<!-- Example: Constitution supersedes all other practices; Amendments require documentation, approval, migration plan -->

Esta constitución prevalece sobre prácticas locales contradictorias del frontend. Toda
enmienda MUST describir el motivo, el impacto sobre páginas, contratos, seguridad,
tests y documentación, y actualizar el Sync Impact Report. La versión usa Semantic
Versioning: MAJOR para eliminar o redefinir reglas de forma incompatible, MINOR para
añadir o ampliar principios o secciones, y PATCH para aclaraciones sin cambio
semántico. Toda revisión de feature MUST verificar el cumplimiento de esta
constitución; cualquier excepción MUST quedar documentada y aprobada en el plan.

La revisión de cumplimiento se realiza antes de integrar cambios y nuevamente al
modificar contratos OpenAPI, autenticación, estado global, accesibilidad o el sistema
visual. La definición de terminado exige que build, lint y tests pasen, además de una
comprobación visual contra el mockup.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): confirmar fecha original de adopción | **Last Amended**: 2026-09-23
