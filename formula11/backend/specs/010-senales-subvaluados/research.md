# Research: 010 - Detector de jugadores subvaluados

## Hallazgos del código

- `RecalculoSemanaService.procesar(domingo)` borra y vuelve a insertar las cotizaciones de la semana: sus ids cambian en cada recálculo. Por eso las señales no referencian cotizaciones; guardan los precios.
- Los jugadores que no jugaron una semana quedan con score `0` y conservan el precio anterior. El score solo no alcanza para saber si un jugador jugó; se usa `metrica_partido.minutos > 0`.
- El precio semanal está limitado a ±20 % (`topeVariacion`). Un jugador que empieza a rendir mucho tarda varias semanas en alcanzar su precio "justo": es el caso que detecta `PRECIO_ATRASADO`.
- Las dos estrategias se calculan siempre juntas para todos los jugadores, así que en cada fecha hay precio de las dos. La diferencia entre ambas es una segunda opinión sobre el valor del jugador (`ENTRE_ESTRATEGIAS`).
- `RecalculoServiceTest` construye `RecalculoService` con 5 argumentos. Para no modificarlo, se conserva ese constructor y se agrega otro con el puerto.
- `POST /api/ordenes/compra` ya delega en `MercadoService` y se puede usar desde una alerta sin cambios.
- La exclusión de cobertura de Sonar incluye `**/model/**`: las clases puras del modelo se prueban igual, pero la cobertura exigida recae en services y controllers.

## Decisiones

| Tema | Decisión | Alternativa descartada |
|---|---|---|
| Momento de generación | Al final de cada semana del recálculo, mediante un puerto | Generar al leer: las señales cambiarían entre consultas y no habría ids estables |
| Precio estimado de `PRECIO_ATRASADO` | Mediana de pares de la misma posición con score parecido | Promedio: sensible a pocos precios muy altos |
| Ventana de pares | Score ±0,05 y mínimo 5 pares | Percentiles: más difícil de explicar en la demo |
| Cantidad | Máximo 20 por tipo y semana | Sin límite: cientos de alertas por semana después de varias semanas de divergencia |
| Estabilidad de ids | Upsert por jugador, semana y tipo | Borrar e insertar: el frontend repetiría alertas ya vistas |
| Medición | Variación a una semana contra el promedio de todo el mercado | Contra la posición: el contrato habla del "promedio del mercado" |
