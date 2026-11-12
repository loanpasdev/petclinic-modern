# 03 - Plan de migración

## 1. Alcance

Obligatorio: propietarios, mascotas, visitas y veterinarios expuestos por API REST, con las reglas
de negocio del legacy preservadas y probadas. Java 21+, OpenAPI, pruebas, documentación y GitFlow.

Opcional incluido: SPA estática de propietarios, mascotas, visitas y veterinarios, sin build ni
dependencias externas, servida por la propia aplicación.

Fuera de alcance: autenticación, Kafka, Kubernetes, API Gateway, CQRS y cualquier componente
distribuido sin necesidad.

## 2. Matriz original vs. nuevo

| Operación original | Endpoint nuevo | Reglas preservadas |
|---|---|---|
| `GET /owners?lastName=&page=` | `GET /api/v1/owners?lastName=&page=&size=` | prefijo de apellido, trim, página 1-based, 5 por defecto |
| `GET /owners/{id}` | `GET /api/v1/owners/{id}` | 404 si no existe; incluye mascotas |
| `POST /owners/new` | `POST /api/v1/owners` | nombre/apellido ≤30, address ≤255, city ≤80, teléfono 10 dígitos |
| `POST /owners/{id}/edit` | `PUT /api/v1/owners/{id}` | mismas validaciones + 404 |
| `POST /owners/{id}/pets/new` | `POST /api/v1/owners/{id}/pets` | nombre ≤30, tipo obligatorio, fecha obligatoria, único por dueño (409) |
| `POST /owners/{id}/pets/{petId}/edit` | `PUT /api/v1/owners/{id}/pets/{petId}` | mismas validaciones + pertenencia al dueño |
| `POST .../visits/new` | `POST /api/v1/owners/{id}/pets/{petId}/visits` | descripción ≤255, fecha obligatoria (por defecto mañana), 404 si no existe la mascota |
| `GET /vets.html`, `GET /vets` | `GET /api/v1/vets` | lista con especialidades |
| `GET /oups` | no migrado | controlador de demostración de errores |

## 3. Secuencia incremental

| Fase | Contenido | Criterio de cierre |
|---|---|---|
| 0 | Repo propio, `main`/`develop`/`feature/*`, Java 21 | push verificado |
| A | `docs/01` a `docs/04` | documentos publicados |
| B | Esqueleto multi-módulo (5 módulos) | `mvn verify` verde sin lógica nueva |
| C1 | Módulo owners (API, DTO, validación, errores) | CRUD + búsqueda probados |
| C2 | Módulo visits (puerto `PetLookup`) | alta y consulta probadas |
| C3 | Módulo vets | consulta con especialidades |
| C4 | OpenAPI + ProblemDetail + paginación | Swagger UI navegable |
| D | Pruebas unitarias, web e integración | `mvn verify` desde clon limpio |
| E | README, colección HTTP, bitácora IA | reproducible por el evaluador |
| F | `release/1.0.0` → `main`, tag `v1.0.0` | entrega enviada |

## 4. Estrategia de datos

- H2 en memoria como configuración por defecto: cero setup para el evaluador.
- `schema.sql` + `data.sql` existentes como fuente del esquema y de los datos de ejemplo.
- MySQL/PostgreSQL opcionales mediante perfiles y `docker-compose.yml`.
- Sin Flyway/Liquibase: se documenta como deuda y se mantiene el mecanismo actual.
- Pruebas de integración contra H2; los perfiles externos se validan manualmente si hay Docker.

## 5. Estrategia de pruebas

| Nivel | Herramienta | Cobertura |
|---|---|---|
| Unitarias | JUnit 5 + Mockito | validadores, servicios, mappers |
| Web | `@WebMvcTest` | códigos HTTP, validación, `ProblemDetail` |
| Persistencia | `@DataJpaTest` | búsqueda por apellido, unicidad de mascota |
| Integración | `@SpringBootTest` + MockMvc | flujos completos owners/pets/visits/vets |
| Calidad | Dependencias de Maven | límites de módulo (sin dependencias cruzadas) |

Comando único: `./mvnw verify`.

## 6. GitFlow

- `main`: entregable estable.
- `develop`: integración.
- `feature/01-docs-diagnostico`, `feature/02-modulos`, `feature/03-owners-api`,
  `feature/04-visits-api`, `feature/05-vets-api`, `feature/06-openapi`, `feature/07-tests`,
  `feature/08-docs-finales`.
- `release/1.0.0` y tag `v1.0.0` al cierre.
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
- Integración con `--no-ff` desde cada feature a `develop`.

## 7. Riesgos y mitigación

| Riesgo | Prob. | Mitigación |
|---|---|---|
| Incompatibilidad springdoc/Boot 4 | Media | spike temprano; fallback a OpenAPI estático |
| Tiempo insuficiente | Alta | priorizar owners + visits + vets; frontend estático solo al final |
| Pruebas legacy rotas | Media | mantener módulo web; reemplazar pruebas MVC por REST |
| Pérdida de reglas de negocio | Media | matriz de la sección 2 + un test por regla |
| Entorno del evaluador | Baja | H2 por defecto y README con clon limpio |

## 8. Desviaciones (a confirmar al cierre)

- Frontend MVC/Thymeleaf retirado: el documento permite eliminar la capa web server-side. Se añade
  como extra una SPA estática sin build que consume la API; el alcance evaluado sigue siendo backend.
- `/oups` no se migra (controlador de demostración de errores).
- Sin Flyway ni migraciones versionadas.
- Sin autenticación ni autorización.
- Los tests de Testcontainers (MySQL/PostgreSQL) se retiran para que `mvn verify` funcione sin
  Docker en un clon limpio; los perfiles de BD quedan documentados y disponibles.

## 9. Definition of Done

1. `./mvnw verify` verde con Java 21 desde un clon limpio.
2. Swagger UI y `/v3/api-docs` accesibles.
3. Flujos de owners, pets, visits y vets demostrados con curl.
4. Reglas de negocio cubiertas por pruebas.
5. `README.md` reproducible y `docs/01`–`04` publicados.
6. `main` con tag `v1.0.0` y `develop` integrado.
7. Bitácora de IA con validaciones humanas.
8. Sin secretos ni credenciales en el repositorio.
