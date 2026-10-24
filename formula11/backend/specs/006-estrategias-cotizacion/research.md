# Research: Estrategias de valuación y cotización semanal

## Decisiones de diseño

### Normalización

Cada métrica se promedia por partido para cada jugador dentro de la semana. Luego se calcula el z-score dentro del grupo de la misma posición y considerando todas las ligas:

```text
z = (valor - promedio_del_grupo) / desvío_estándar_del_grupo
z_acotado = max(-2, min(2, z))
score = (z_acotado + 2) / 4
```

Cuando el grupo tiene un único jugador o desvío estándar cero, el score es `0,5`. Esto evita división por cero y evita que un grupo sin variación produzca ventajas artificiales.

Los jugadores sin partidos en la semana no ingresan al grupo y conservan el precio anterior. En `CONSISTENCIA`, los jugadores con menos de cuatro partidos en la ventana no ingresan al grupo de normalización de esa estrategia y usan directamente el score normalizado de `RENDIMIENTO_POSICION`.

### Justificación de los pesos iniciales

- **DEL** prioriza goles (`0,30`) y rating (`0,20`), con tiros y asistencias como señales ofensivas complementarias. Pases clave y regates completan la creación y progresión. Amarillas, rojas y menos de 90 minutos penalizan disciplina y disponibilidad.
- **MED** da mayor peso a asistencias y pases clave (`0,20` cada uno), mantiene rating alto (`0,25`) y combina producción ofensiva con regates, entradas e intercepciones para representar el aporte mixto del mediocampo.
- **DEF** prioriza entradas (`0,25`) e intercepciones (`0,25`), con rating (`0,30`) como señal global; goles, asistencias y pases clave tienen peso menor para reconocer aportes poco frecuentes. Tarjetas y menos de 90 minutos penalizan riesgo y disponibilidad.
- **POR** concentra el peso en rating (`0,40`), atajadas (`0,35`) y valla invicta (`0,25`), mientras goles recibidos, amarillas y rojas restan valor. No se agregan métricas ofensivas que no representan el rol principal.
- **CONSISTENCIA** suma titularidad (`0,5`) y resta suplencias (`-0,5`) para premiar continuidad de participación; su fórmula base ya mide disponibilidad y estabilidad del rating mediante `lambda`.

Estos pesos se aplican después de estandarizar cada métrica, por lo que sus magnitudes son comparables aunque las unidades originales sean distintas. Las versiones futuras son inmutables y deben conservar el snapshot usado por cada cotización.

### Precio

El precio de partida del momento cero es `1` crédito. Para cada primera semana y para las siguientes se aplica:

```text
precio_t = precio_anterior * (1 + k * (score - 0,5))
```

El resultado se limita a una variación semanal de `±20 %`, respeta el piso configurado de `0,10` créditos y se redondea a cuatro decimales con `HALF_UP`. La primera cotización informa variación `null`, aunque su precio ya haya sido calculado con la fórmula desde el precio de partida.

### Semana y Clock

La semana es lunes–domingo en `America/Argentina/Buenos_Aires`. La fecha persistida de una cotización es el domingo de cierre. El recálculo sin cuerpo procesa el domingo anterior a la fecha del `Clock`; el scheduler corre el lunes a las 06:00 por cron configurable. Todo service dependiente de fecha/hora recibe `Clock`.

### Actualización de métricas

`ActualizacionMetricasPort` permite desacoplar el scheduler de WhoScored. Si no hay implementación o falla la actualización, el job registra el evento y recalcula con las métricas persistidas. Una ejecución posterior del superusuario puede reemplazar esa semana.

### Estado actual del backend

- Dependencias requeridas ya están en [pom.xml](../../pom.xml); no se requiere instalar nada.
- Las migraciones actuales son V1–V3, por lo que V10–V14 deben diseñarse como evolución posterior compatible.
- La caché existente en [FootballCacheConfig.java](../../src/main/java/com/formula11/config/FootballCacheConfig.java) usa un bean Caffeine genérico; la caché de cotización debe usar una clave/tipo propio para evitar colisiones.
- El filtro JWT actual crea authorities vacías; la autorización por rol se probará con `@WithMockUser` y la integración del claim `SUPERUSUARIO` se coordinará con la feature 007.
