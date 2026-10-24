# Research: Detalle de jugador con evolución de cotizaciones

**Feature**: `003-detalle-cotizaciones`  
**Date**: 2026-10-24

## Contract source

- **Decision**: usar como autoridad las rutas y nombres castellanos del contrato de
  Entrega 2 descrito en `spec.md`.
- **Rationale**: la constitución exige que el frontend no invente endpoints; las rutas
  inglesas del documento de visión son contexto histórico, no contrato ejecutable.
- **Alternatives considered**: conservar `/players/:id` y `/players/:id/quotes`;
  descartado porque contradice las rutas `/api/jugadores/...` del contrato vigente.

## Protected access and errors

- **Decision**: todas las consultas pasan por `solicitarProtegida`.
- **Rationale**: centraliza Bearer, `X-Correlation-Id`, normalización de `mensaje`,
  `correlationId` y limpieza de sesión ante `401`, en línea con la constitución.
- **Alternatives considered**: `fetch` desde componentes o hooks sin cliente común;
  descartado porque rompe las capas y la trazabilidad.

## Role-based strategy visibility

- **Decision**: `USUARIO` consulta la estrategia activa sin parámetro; `SUPERUSUARIO`
  puede consultar las dos estrategias y además consulta `GET /api/estrategias` para
  marcar la que tiene `activa: true`.
- **Rationale**: coincide con la clarificación de la spec y evita exponer una estrategia
  no activa al público.
- **Alternatives considered**: mostrar ambas estrategias a todos; descartado por
  autorización contractual.

## Partial failures

- **Decision**: conservar la ficha si falla el historial y mostrar el error normalizado
  en su panel; el error de la ficha impide mostrar el panel de cotizaciones.
- **Rationale**: mantiene el valor disponible y coincide con los escenarios de la spec
  y el uso de `Promise.allSettled`.
- **Alternatives considered**: fallar toda la pantalla ante cualquier error; descartado
  porque oculta datos válidos.

## Response normalization

- **Decision**: el contrato canónico es `{ jugadorId, estrategia, cotizaciones }`;
  cualquier compatibilidad adicional (`content` o lista) debe conservarse solo si está
  justificada por mocks existentes y no altera el contrato documentado.
- **Rationale**: evita que la tolerancia del frontend oculte respuestas inválidas del
  backend. La implementación actual requiere revisar FR-003 porque no cubre literalmente
  todas las formas que declara.
- **Alternatives considered**: aceptar cualquier forma sin validación; descartado por
  riesgo de datos silenciosamente incorrectos.

## Visualization and accessibility

- **Decision**: cargar Recharts de forma diferida y usar la tabla de historial como
  alternativa textual del gráfico; formatear fechas por componentes, sin `Date`.
- **Rationale**: reduce el bundle inicial, funciona en jsdom y evita desplazamientos de
  día por zona horaria.
- **Alternatives considered**: dibujar el gráfico en el bundle inicial o usar `Date`
  para fechas de cotización; descartado por rendimiento y correcciones de fecha.

## Remaining implementation dependency

`GET /api/estrategias`, su endpoint y sus handlers MSW aún no aparecen en la
implementación existente. Se mantiene como tarea pendiente T018; no se considera una
decisión abierta del diseño porque la spec ya fija su forma y propósito.
