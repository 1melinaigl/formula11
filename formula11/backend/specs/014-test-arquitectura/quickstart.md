# Quickstart: test de arquitectura

## Ejecución

Desde `backend`, ejecutá el test por separado o dentro de las ejecuciones habituales:

```powershell
mvn test -Punitarios -Dtest=ArquitecturaTest
mvn test -Punitarios
mvn test
mvn verify
```

El primer comando ejecuta `ArquitecturaTest` dentro del perfil que incluye `**/unit/**`. El test es unitario: no inicia Spring ni necesita una base de datos. `mvn verify` es la ejecución usada por CI.

## Validación manual de las reglas

Durante la implementación, validá cada regla FR-002 a FR-010 de forma aislada:

1. Introducí temporalmente en código productivo una dependencia, anotación, nombre, ciclo o ubicación de paquete que infrinja la regla que estás comprobando.
2. Ejecutá `mvn test -Punitarios -Dtest=ArquitecturaTest`.
3. Confirmá que el test falla e identifica la regla y la infracción introducida.
4. Revertí inmediatamente solo el cambio temporal antes de comprobar otra regla.

No agregues clases de prueba que violen reglas a propósito. Al terminar cada comprobación, verificá que no quede ninguna infracción temporal y ejecutá la suite unitaria completa. Si el test detecta una infracción que ya existía en el código productivo, corregila en el código; no la ocultes con una excepción.
