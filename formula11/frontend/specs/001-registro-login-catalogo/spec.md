# Feature Specification: Registro, login y catálogo de jugadores

**Feature Branch**: `001-registro-login-catalogo`

**Created**: 2026-09-23

**Status**: Draft

**Input**: User description: "Frontend de Formula11, Entrega 1: registro, login y catálogo de jugadores, con la estética retro de design/formula11-mockup-retro-v3.html (referencia visual y de comportamiento obligatoria)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navegar el menú y conocer el estado de sesión (Priority: P1)

Como visitante, quiero recorrer el menú principal con el teclado para identificar las
opciones disponibles y saber si tengo una sesión activa.

**Why this priority**: El menú es la puerta de entrada a todas las capacidades de la entrega.

**Independent Test**: Se puede probar desde la pantalla inicial usando solo flechas, Enter y
Esc, con y sin sesión, y verificar cada estado visible.

**Acceptance Scenarios**:

1. **Given** una persona sin sesión en el menú, **When** navega con las flechas, **Then**
   puede seleccionar "Ingresar / Crear cuenta" y "Catálogo de jugadores", mientras
   "Ranking" y "Mercado de tokens" aparecen bloqueados con la etiqueta "PRÓXIMAMENTE".
2. **Given** una persona sin sesión, **When** observa el encabezado, **Then** ve el estado
   "Sin sesión".
3. **Given** una persona con sesión, **When** observa el menú, **Then** ve "Cerrar sesión"
   y el chip del nombre de la sesión activa, y el catálogo permanece visible.
4. **Given** una opción seleccionada, **When** pulsa Enter, **Then** se abre esa opción;
   cuando pulsa Esc en una vista secundaria, vuelve al menú.

---

### User Story 2 - Ingresar con una cuenta existente (Priority: P1)

Como usuario registrado, quiero ingresar con mi email y contraseña para acceder al catálogo.

**Why this priority**: La autenticación es necesaria para consultar los jugadores protegidos.

**Independent Test**: Se puede probar con credenciales válidas, inválidas y datos con formato
incorrecto, verificando el destino y los mensajes mostrados.

**Acceptance Scenarios**:

1. **Given** la vista de autenticación, **When** el usuario elige la pestaña de ingreso,
   completa email y contraseña válidos y confirma, **Then** la sesión queda iniciada y se
   muestra el catálogo.
2. **Given** la vista de autenticación, **When** el servidor rechaza las credenciales,
   **Then** se muestra el mensaje recibido junto con su `correlationId` y no se inicia sesión.
3. **Given** la vista de autenticación, **When** los datos tienen un formato inválido,
   **Then** se muestra el aviso de validación correspondiente y no se envía información
   incompleta.
4. **Given** un usuario autenticado, **When** recarga la página, **Then** la sesión se
   conserva sin mostrar el token en ninguna pantalla.

---

### User Story 3 - Crear una cuenta (Priority: P1)

Como visitante, quiero crear una cuenta con mis datos para comenzar a usar Formula11.

**Why this priority**: Permite que una persona nueva obtenga acceso sin depender de cuentas
preconfiguradas.

**Independent Test**: Se puede probar el formulario con datos válidos, límites de longitud,
formatos inválidos y un email ya registrado.

**Acceptance Scenarios**:

1. **Given** la vista de autenticación, **When** el usuario cambia a "Crear cuenta" y envía
   nombre, email y contraseña válidos, **Then** se muestra la cuenta creada con id, nombre,
   email y fecha de registro, la sesión queda iniciada y aparece el botón "Ir al catálogo".
2. **Given** el formulario de registro, **When** un campo incumple sus reglas, **Then** el
   error se muestra debajo de ese campo y el formulario no se completa.
3. **Given** el formulario de registro, **When** el servidor responde con datos inválidos o
   email ya registrado, **Then** se muestra el mensaje del servidor y su `correlationId`.
4. **Given** una cuenta creada, **When** el usuario revisa la confirmación, **Then** nunca
   ve el token.

---

### User Story 4 - Buscar y consultar el catálogo (Priority: P1)

Como usuario con sesión, quiero buscar y filtrar jugadores para consultar rápidamente sus
datos básicos.

**Why this priority**: El catálogo es el resultado principal de esta primera entrega.

**Independent Test**: Se puede probar con un catálogo con resultados, un catálogo vacío,
búsqueda, filtros combinados y una pantalla pequeña.

**Acceptance Scenarios**:

1. **Given** una sesión activa y un catálogo disponible, **When** el usuario busca por nombre
   o aplica filtros de liga, posición y equipo, **Then** la lista muestra solo los jugadores
   coincidentes, los filtros activos aparecen como chips y se muestra un contador.
2. **Given** filtros activos, **When** el usuario pulsa "Limpiar", **Then** se eliminan los
   filtros y vuelve a mostrarse el catálogo completo.
3. **Given** un jugador en la lista, **When** el usuario lo selecciona, **Then** ve una ficha
   con id, equipo, liga y posición.
4. **Given** una pantalla de hasta 760px de ancho, **When** el usuario alterna entre lista y
   ficha, **Then** puede volver a la lista mediante un botón claramente identificable.
5. **Given** una respuesta de catálogo sin jugadores, **When** termina la consulta, **Then**
   se muestra un estado vacío comprensible.
6. **Given** una sesión ausente o vencida, **When** el usuario intenta consultar el catálogo,
   **Then** ve "Acceso denegado", el mensaje del servidor y su `correlationId`, y puede pulsar
   un botón para ingresar.
7. **Given** una sesión activa, **When** el usuario elige "Cerrar sesión", **Then** vuelve al
   menú, el encabezado muestra "Sin sesión" y el catálogo deja de estar disponible.

### Edge Cases

- El nombre debe admitir entre 1 y 100 caracteres; los valores fuera de ese rango se
  rechazan antes de completar el registro.
- El email debe tener formato válido y como máximo 254 caracteres; los espacios externos
  deben tratarse según la regla del servicio.
- La contraseña debe admitir entre 8 y 72 caracteres y no debe aparecer en mensajes ni
  confirmaciones.
- El usuario puede recibir simultáneamente un error de formato local y un error del servicio;
  el aviso de cada campo conserva su contexto y el aviso general conserva el mensaje y
  `correlationId` del servicio.
- Un `401` al consultar el catálogo invalida la sesión local y devuelve al estado de acceso
  denegado.
- Un catálogo vacío no se confunde con un error de conexión.
- Una búsqueda sin coincidencias muestra un estado vacío sin inventar jugadores.
- No existen cuentas demo; las credenciales mostradas en la referencia visual no se pueden
  usar para ingresar.
- Ranking, mercado de tokens, cotizaciones, transacciones y recuperación de contraseña no
  ofrecen acciones en esta entrega.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST mostrar un menú principal con "Ingresar / Crear cuenta" o
  "Cerrar sesión" según exista una sesión activa.
- **FR-002**: El sistema MUST mantener "Catálogo de jugadores" visible en el menú para todas
  las personas.
- **FR-003**: El sistema MUST mostrar "Ranking" y "Mercado de tokens" como opciones
  bloqueadas con la etiqueta "PRÓXIMAMENTE".
- **FR-004**: El sistema MUST permitir navegar las opciones y acciones principales con
  flechas, Enter y Esc, y MUST mostrar foco visible.
- **FR-005**: El sistema MUST permitir alternar entre las pestañas "Ingresar" y "Crear
  cuenta".
- **FR-006**: El sistema MUST aceptar el ingreso mediante email y contraseña y dirigir a la
  persona autenticada al catálogo.
- **FR-007**: El sistema MUST validar el email y la contraseña antes de enviar el formulario
  y MUST mostrar los errores de formato junto al campo correspondiente.
- **FR-008**: El sistema MUST mostrar el mensaje recibido para respuestas de credenciales
  inválidas o datos inválidos junto con su `correlationId`.
- **FR-009**: El sistema MUST permitir crear una cuenta con nombre, email y contraseña,
  respetando los límites definidos por el contrato del servicio.
- **FR-010**: El sistema MUST mostrar los errores de registro bajo el campo afectado cuando
  sean errores de forma y como aviso general cuando provengan del servicio.
- **FR-011**: El sistema MUST mostrar id, nombre, email y fecha de registro después de crear
  una cuenta, y MUST ofrecer "Ir al catálogo".
- **FR-012**: El sistema MUST mantener la sesión activa después de recargar la página y
  MUST mostrar el nombre de la persona activa en el encabezado.
- **FR-013**: El sistema MUST NOT mostrar ni registrar el token de sesión en ninguna vista,
  mensaje o salida visible para la persona usuaria.
- **FR-014**: El sistema MUST permitir consultar el catálogo solo con una sesión válida.
- **FR-015**: El sistema MUST ofrecer búsqueda por nombre y filtros por liga, posición y
  equipo, mostrando los filtros activos como chips.
- **FR-016**: El sistema MUST mostrar un contador, una lista de resultados y una ficha con id,
  equipo, liga y posición del jugador seleccionado.
- **FR-017**: El sistema MUST ofrecer la acción "Limpiar" para retirar todos los filtros
  aplicados.
- **FR-018**: El sistema MUST representar por separado un catálogo vacío y una búsqueda sin
  coincidencias, sin crear datos que no provengan del servicio.
- **FR-019**: El sistema MUST mostrar una pantalla de "Acceso denegado" ante una sesión
  ausente o vencida, incluir el mensaje del servicio y `correlationId`, y ofrecer ingresar.
- **FR-020**: El sistema MUST limpiar la sesión y volver al menú cuando la persona cierre
  sesión o el servicio responda `401` en una consulta protegida.
- **FR-021**: El sistema MUST adaptar la consulta del catálogo a pantallas pequeñas de hasta
  760px, alternando lista y ficha con una acción para volver.
- **FR-022**: La experiencia MUST usar español en textos, avisos y errores, y MUST preservar
  la estética retro definida por la referencia visual: cancha de fondo, scanlines, paleta
  verde/dorada, Bebas Neue, Barlow, paneles y botones inclinados.
- **FR-023**: El sistema MUST asociar cada etiqueta con su campo y MUST conservar un foco
  visible en controles navegables.

### Key Entities

- **Cuenta**: identidad registrada con id, nombre, email y fecha de registro.
- **Sesion**: estado de acceso activo asociado al nombre de la cuenta; su credencial nunca
  se expone a la persona usuaria.
- **Jugador**: elemento del catálogo con id, nombre, equipo, liga y posición.
- **Filtro de catalogo**: criterios opcionales de nombre, liga, posición y equipo que reducen
  los resultados visibles.
- **Error de servicio**: aviso con mensaje, fecha y `correlationId` para explicar rechazos o
  fallos de acceso.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Al menos el 95% de las personas de prueba puede llegar desde el menú al
  catálogo usando teclado, sin usar el mouse, en menos de 60 segundos.
- **SC-002**: Al menos el 95% de los intentos válidos de ingreso o registro llega a su estado
  siguiente correcto en menos de 3 interacciones posteriores al envío.
- **SC-003**: El 100% de los casos de error `400`, `401` y `409` muestra el mensaje del
  servicio y su `correlationId` sin exponer el token.
- **SC-004**: El 100% de las sesiones válidas probadas permanece disponible después de una
  recarga y el 100% de las sesiones vencidas vuelve al estado de acceso denegado.
- **SC-005**: El 95% de las personas de prueba puede encontrar un jugador aplicando nombre o
  filtros en menos de 30 segundos cuando el catálogo contiene coincidencias.
- **SC-006**: El catálogo vacío y la búsqueda sin coincidencias se reconocen correctamente
  en el 100% de las pruebas, sin presentar jugadores inventados.
- **SC-007**: La interfaz conserva la lectura y la operación de lista y ficha en el 100% de
  las pruebas realizadas en anchos de pantalla de 760px o menos.
- **SC-008**: El 100% de los controles interactivos tiene etiqueta asociada, foco visible y
  recorrido de teclado verificable.
- **SC-009**: En una revisión visual, la pantalla coincide con la referencia retro en
  estructura, paleta, tipografía, paneles, botones inclinados, scanlines y fondo de cancha,
  salvo desviaciones justificadas en el plan.

## Assumptions

- La entrega consume el servicio existente de Formula11 y sus contratos vigentes para
  registro, ingreso y catálogo; no se agregan capacidades que el servicio no ofrezca.
- La respuesta de error del servicio contiene un mensaje, una fecha y un `correlationId`.
- La sesión puede persistir de forma segura entre recargas sin que la credencial sea visible
  ni se escriba en logs.
- Las ligas disponibles son las definidas por el servicio; la interfaz no agrega valores
  propios.
- La referencia `design/formula11-mockup-retro-v3.html` es la autoridad visual y de
  comportamiento para esta entrega.
- Ranking, mercado de tokens, cotizaciones, transacciones y recuperación de contraseña son
  dependencias futuras y no forman parte del alcance actual.
