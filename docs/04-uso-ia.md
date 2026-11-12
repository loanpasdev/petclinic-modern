# 04 - Bitácora de uso de IA

Registro de herramientas, objetivos, prompts representativos, resultados aceptados o rechazados y
validación humana. La responsabilidad del código entregado es del candidato.

## 1. Herramientas utilizadas

| Herramienta | Modelo / versión | Uso |
|---|---|---|
| OpenCode (agente de código) | big-pickle | análisis del repositorio, diagnóstico, redacción de documentos, generación y revisión de código y pruebas |
| ChatGPT / Claude (consulta puntual) | según disponibilidad | dudas de compatibilidad de versiones |

## 2. Bitácora

| # | Fecha | Objetivo | Prompt representativo (resumen) | Resultado | Validación humana |
|---|---|---|---|---|---|
| 1 | 2026-11-11 | Analizar el PDF de la prueba | "Lee PRUEBA_TECNICA_JAVA_PETCLINIC.pdf y extrae requisitos, criterios y checklist" | Extracción de 12 páginas y resumen de criterios | Lectura cruzada del PDF original |
| 2 | 2026-11-11 | Diagnosticar el legacy | "Revisa el proyecto spring-petclinic: stack, componentes, BD, validaciones y deuda técnica" | `docs/01-diagnostico-legacy.md` | Verificación con `pom.xml`, `application*.properties`, `schema.sql` y clases |
| 3 | 2026-11-11 | Decidir arquitectura | "¿Monolito modular o microservicios para 1 día con estas entidades?" | Monolito modular Maven multi-módulo | Contraste con el documento de la prueba y las relaciones JPA reales |
| 4 | 2026-11-11 | Preparar repositorio | "Configura upstream/origin y GitFlow sin perder historial" | Secuencia de comandos Git | Verificación con `git remote -v`, `git branch -a` y `git ls-remote` |
| 5 | 2026-11-11 | Resolver autenticación Git | "Error 403 al hacer push como otra cuenta" | Diagnóstico de credencial almacenada | Comprobación con `git credential-manager github list` y `cmdkey /list` |
| 6 | 2026-11-12 | Dividir en módulos Maven | "Convierte el proyecto en multi-módulo sin romper reglas JPA" | 5 módulos (`common`, `owners`, `visits`, `vets`, `app`), `Visit` con `petId` y sin colección en `Pet` | `mvn test` verde; se corrigieron a mano los imports JAXB y la caché JCache |
| 7 | 2026-11-12 | Diseñar la API REST | "Expón owners/pets/visits/vets con DTO, validación y ProblemDetail" | Controladores, servicios, DTO y `GlobalExceptionHandler` | Pruebas MockMvc y smoke tests con `curl` contra H2 |
| 8 | 2026-11-12 | Ajustar pruebas legacy | "Arregla las pruebas que dependían de la capa MVC y de `Pet.getVisits()`" | Se retiraron las pruebas MVC/Testcontainers y se reescribieron las de persistencia | `mvn verify` verde sin Docker |
| 9 | 2026-11-12 | Cerrar documentación | "Actualiza README, ejemplos HTTP y bitácora" | `README.md`, `docs/05`, actualización de `docs/02`–`docs/04` | Revisión cruzada con el PDF y el código |

## 3. Sugerencias aceptadas, rechazadas y corregidas

- **Aceptada:** H2 en memoria como configuración por defecto para el evaluador.
- **Aceptada:** monolito modular con módulos `owners`, `visits`, `vets` y `common`.
- **Rechazada:** microservicios por entidad (consistencia y tiempo).
- **Rechazada:** Flyway/Liquibase (amplía alcance sin aportar a la evaluación).
- **Corregida:** el primer plan ubicaba el repositorio como tarea de "setup" y no como bloqueo
  previo; el documento exige publicar el repo y las ramas antes de modificar código.
- **Corregida:** se asumía que la consola H2 estaba habilitada; la configuración real no la activa.

## 4. Validación técnica aplicada

- Compilación y arranque del proyecto original con Java 21 (`mvnw package`, `java -jar`).
- Verificación de endpoints originales (`/`, `/owners/find`, `/vets.html`, `/actuator/health`).
- Lectura directa de entidades, validadores y scripts SQL para confirmar cada regla.
- Ejecución de la suite de pruebas en cada fase (`./mvnw verify`).
- Revisión de cada documento contra el código, no solo contra la salida del modelo.

## 5. Limitaciones y errores detectados en la IA

- El modelo no puede leer PDF directamente; fue necesario extraer el texto con `pypdf`.
- Propuso inicialmente un orden de trabajo que no respetaba el bloqueo de repositorio del PDF.
- Asumió que `spring.h2.console.enabled` estaba configurado, lo cual era falso.
- No puede verificar credenciales de GitHub; el diagnóstico se basó en la salida real de Git.

## 6. Buenas prácticas aplicadas

- No se publicaron credenciales, tokens ni datos sensibles en prompts ni en el repositorio.
- Toda sugerencia de código se contrasta con las reglas del legacy y con pruebas ejecutables.
- Las decisiones arquitectónicas se registran en `docs/02-arquitectura.md` con sus alternativas.
