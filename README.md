# PetClinic Modernizado — Prueba Técnica Java Backend

Reescritura del proyecto legacy [Spring PetClinic](https://github.com/spring-projects/spring-petclinic)
como **monolito modular Maven** con **Java 21**, **Spring Boot 4.1** y una **API REST** documentada con
OpenAPI. Las reglas de negocio del legacy se conservan y están cubiertas por pruebas automatizadas.

- Repositorio: <https://github.com/loanpasdev/petclinic-modern>
- Documentación de la prueba: [`docs/01`](docs/01-diagnostico-legacy.md) ·
  [`docs/02`](docs/02-arquitectura.md) · [`docs/03`](docs/03-plan-migracion.md) ·
  [`docs/04`](docs/04-uso-ia.md)
- Ejemplos de llamadas: [`docs/05-api-ejemplos.http`](docs/05-api-ejemplos.http)

## 1. Stack

| Componente | Versión |
|---|---|
| Java | 21 (`maven.compiler.release=21`) |
| Spring Boot | 4.1.0 |
| Spring Data JPA / Hibernate | 7.4.1 |
| H2 (por defecto, en memoria) | 2.4.240 |
| springdoc-openapi | 3.2.0 |
| Maven Wrapper | 3.9.16 |
| JUnit 5 + Mockito + MockMvc | incluidos en los starters de test |
| Interfaz web | HTML + CSS + JavaScript vanilla, sin build ni dependencias |

## 2. Arquitectura

Monolito modular con cinco módulos Maven y límites explícitos:

```
petclinic-common   modelo base, errores comunes, paginación
petclinic-owners   Owner, Pet, PetType + API /api/v1/owners y /api/v1/pet-types
petclinic-visits   Visit + API /api/v1/owners/{ownerId}/pets/{petId}/visits
petclinic-vets     Vet, Specialty + API /api/v1/vets
petclinic-app      main, configuración, OpenAPI, manejo global de errores
```

Reglas de dependencia:

- `common` no depende de ningún otro módulo.
- `owners`, `visits` y `vets` dependen solo de `common`; `visits` además consume el puerto público
  `PetQueryService` de `owners` (no accede a sus repositorios).
- `app` es el único que ensambla todos los módulos y contiene el `main`.

Detalle y alternativas descartadas en [`docs/02-arquitectura.md`](docs/02-arquitectura.md).

## 3. Cómo ejecutar

Requisitos: **JDK 21** y conexión a Maven Central la primera vez.

```bash
git clone https://github.com/loanpasdev/petclinic-modern.git
cd petclinic-modern
```

### Opción A — empaquetar y ejecutar el jar (recomendada)

```bash
./mvnw -B package
java -jar petclinic-app/target/petclinic-app-4.0.0-SNAPSHOT.jar
```

En Windows:

```powershell
.\mvnw.cmd -B package
java -jar .\petclinic-app\target\petclinic-app-4.0.0-SNAPSHOT.jar
```

### Opción B — instalar módulos y usar `spring-boot:run`

`spring-boot:run` debe ejecutarse en el módulo `petclinic-app`, no en el POM raíz, y los módulos
hermanos deben estar instalados antes:

```bash
./mvnw -B install -DskipTests
./mvnw -pl petclinic-app spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd -B install -DskipTests
.\mvnw.cmd -pl petclinic-app spring-boot:run
```

Si el puerto 8080 está ocupado, añade `--server.port=8081` al jar o
`-Dspring-boot.run.arguments=--server.port=8081` a Maven.

La aplicación arranca en <http://localhost:8080> con H2 en memoria y los datos de ejemplo
(`schema.sql` + `data.sql`). No requiere Docker ni base de datos externa. La raíz `/` sirve la
interfaz web; la API vive bajo `/api/v1`.

Comprobación rápida:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/vets
```

## 4. API REST

Base: `/api/v1`. JSON de entrada y salida; nunca se exponen entidades JPA. Los errores usan
`ProblemDetail` (RFC 9457).

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/v1/owners?lastName=&page=1&size=5` | Busca por prefijo de apellido (página 1-based) | 200 |
| GET | `/api/v1/owners/{id}` | Propietario con sus mascotas | 200 |
| POST | `/api/v1/owners` | Crea propietario | 201 + `Location` |
| PUT | `/api/v1/owners/{id}` | Actualiza propietario | 200 |
| GET | `/api/v1/pet-types` | Catálogo de tipos de mascota | 200 |
| POST | `/api/v1/owners/{ownerId}/pets` | Añade mascota | 201 + `Location` |
| PUT | `/api/v1/owners/{ownerId}/pets/{petId}` | Actualiza mascota | 200 |
| GET | `/api/v1/owners/{ownerId}/pets/{petId}/visits` | Visitas de la mascota | 200 |
| POST | `/api/v1/owners/{ownerId}/pets/{petId}/visits` | Registra visita | 201 + `Location` |
| GET | `/api/v1/vets` | Lista de veterinarios con especialidades | 200 |
| GET | `/api/v1/vets/{id}` | Veterinario con especialidades | 200 |

Códigos de error: `400` validación, `404` recurso inexistente, `409` conflicto de negocio
(nombre de mascota duplicado), `500` error no controlado.

### Ejemplos

```bash
# Listar propietarios cuyo apellido empieza por "Dav"
curl "http://localhost:8080/api/v1/owners?lastName=Dav&page=1&size=5"

# Crear propietario
curl -i -X POST http://localhost:8080/api/v1/owners \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ana","lastName":"Garcia","address":"Calle 1","city":"Madrid","telephone":"6001112222"}'

# Añadir mascota (typeId 1 = cat)
curl -i -X POST http://localhost:8080/api/v1/owners/1/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Nina","birthDate":"2020-01-01","typeId":1}'

# Registrar visita sin fecha (se usa mañana)
curl -i -X POST http://localhost:8080/api/v1/owners/6/pets/7/visits \
  -H "Content-Type: application/json" \
  -d '{"description":"revisión"}'

# Listar visitas de la mascota 7 del propietario 6
curl http://localhost:8080/api/v1/owners/6/pets/7/visits
```

Respuesta de validación (ejemplo):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/owners",
  "errors": [{ "field": "telephone", "message": "El número de teléfono debe tener 10 dígitos" }]
}
```

## 5. OpenAPI / Swagger

Con la aplicación en marcha:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Especificación JSON: <http://localhost:8080/v3/api-docs>

La especificación se genera a partir de los controladores y DTO; el título, versión y descripción se
definen en `OpenApiConfiguration`.

## 6. Interfaz web (extra opcional)

La aplicación incluye una SPA estática en `petclinic-app/src/main/resources/static` que Spring Boot
sirve en <http://localhost:8080/>. No requiere Node ni un build aparte: se empaqueta en el mismo jar y
consume la API pública `/api/v1`.

- Propietarios: búsqueda por apellido, paginación, alta y edición, con tarjetas y sus mascotas.
- Mascotas: alta y edición con tipo y fecha de nacimiento.
- Visitas: historial por mascota (botón 📅 en cada mascota) y registro de nuevas visitas.
- Veterinarios: tarjetas con especialidades y filtro por especialidad. El catálogo es de solo
  lectura, igual que en la API: el backend solo expone `GET /api/v1/vets`.

La interfaz consume **los DTO del contrato REST** (`OwnerResponse`, `PetResponse`, `VisitResponse`,
`VetResponse`, `PetTypeResponse`), nunca las entidades JPA. Es la misma frontera que aplica el
backend: los DTO son el contrato público y las entidades de persistencia no se exponen, de modo que
la interfaz puede cambiar sin tocar el dominio.

Si prefieres no usar la interfaz, todos los flujos están disponibles por REST y en Swagger UI.

## 7. Reglas de negocio preservadas

| Regla legacy | Dónde se aplica |
|---|---|
| `firstName`/`lastName` obligatorios, máx. 30 | `OwnerRequest`, `Person` |
| `address` obligatoria, máx. 255; `city` máx. 80 | `OwnerRequest`, `Owner` |
| `telephone` obligatorio, exactamente 10 dígitos | `OwnerRequest` (`@Pattern`) |
| Nombre de mascota obligatorio, máx. 30 | `PetRequest` |
| Tipo de mascota obligatorio al crear | `PetRequest.typeId` + `OwnerService` |
| Fecha de nacimiento obligatoria | `PetRequest.birthDate` |
| Nombre de mascota único por propietario (case-insensitive) | `OwnerService` → `409` + índice único en BD |
| Visita: descripción obligatoria, máx. 255 | `VisitRequest` |
| Visita: fecha futura; por defecto mañana | `VisitRequest` + `VisitService` |
| Búsqueda por prefijo de apellido, con `trim` | `OwnerService.search` |
| Paginación 1-based, tamaño 5 por defecto, fuera de rango → página 1 | `OwnerService.search` |
| Propietario/mascota/veterinario inexistente → 404 | `NotFoundException` |

## 8. Pruebas

```bash
./mvnw verify
```

| Nivel | Clase | Cobertura |
|---|---|---|
| Unitarias | `OwnerServiceTests`, `VisitServiceTests`, `OwnerTests`, `VetTests`, `ValidatorTests` | reglas de servicio y dominio |
| Persistencia | `ClinicServiceTests` | repositorios, unicidad de mascota, visitas |
| Integración REST | `RestApiIntegrationTests` | contrato completo con MockMvc: 200/201/400/404/409, paginación, OpenAPI, actuator |
| Contexto | `PetClinicIntegrationTests` | arranque, datos de ejemplo, caché de vets |

Todas las pruebas usan H2 en memoria y se ejecutan sin Docker.

La aplicación también usa H2 en memoria: al reiniciarla se vuelven a ejecutar `schema.sql` y
`data.sql`, de modo que los datos de ejemplo quedan restablecidos y las pruebas funcionales se
pueden repetir desde cero.

## 9. Perfiles de base de datos

Por defecto: H2 en memoria. Opcionales con Docker:

```bash
docker compose up -d mysql        # o postgres
./mvnw -pl petclinic-app spring-boot:run -Dspring-boot.run.profiles=mysql
```

En Windows, entrecomilla la propiedad para que PowerShell no la parta:

```powershell
docker compose up -d mysql
.\mvnw.cmd -pl petclinic-app spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Variables: `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASS` / `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASS`.

## 10. Estructura del repositorio

```
petclinic-common/   petclinic-owners/   petclinic-visits/   petclinic-vets/   petclinic-app/
docs/               01 diagnóstico · 02 arquitectura · 03 plan · 04 uso de IA · 05 ejemplos HTTP
petclinic-app/src/main/resources/static/   interfaz web (index.html, css, js, img)
pom.xml             parent Maven (Java 21, dependencyManagement)
docker-compose.yml  MySQL y PostgreSQL opcionales
```

## 11. GitFlow

- `main`: entregable estable (tags `v1.0.0`, `v1.0.1`, `v1.0.2`, `v1.0.3`, `v1.0.4`).
- `develop`: integración.
- `feature/01-docs-diagnostico`, `feature/02-modulos`, `feature/03-rest-api`,
  `feature/04-docs-entrega`, `feature/05-frontend`.
- `release/1.0.0` al cierre y `hotfix/1.0.1`, `hotfix/1.0.2` para correcciones puntuales.
- Commits con Conventional Commits (`docs:`, `refactor:`, `feat:`, `test:`), integraciones con
  `--no-ff`.

## 12. Uso de IA

La bitácora completa (herramientas, prompts, resultados aceptados/rechazados y validación humana)
está en [`docs/04-uso-ia.md`](docs/04-uso-ia.md).

## 13. Desviaciones documentadas

- **Frontend MVC/Thymeleaf retirado**: el enunciado permite eliminar la capa web; el alcance
  evaluado es backend.
- **Sin Flyway/Liquibase**: se mantienen los scripts SQL del legacy como deuda aceptada.
- **Sin autenticación**: fuera de alcance del enunciado.
- **Tests de Testcontainers retirados**: para que `mvn verify` funcione en un clon limpio sin Docker;
  los perfiles MySQL/PostgreSQL siguen disponibles.
- **`/oups`** (controlador de demostración de errores) no se migra.
- **CI legacy de Gradle y Kubernetes retirado**: los workflows originales compilaban con Java 17 y
  desplegaban una imagen ajena al repositorio. Se conserva un único workflow Maven (`maven-build.yml`)
  que ejecuta `./mvnw -B verify` con JDK 21 en `main` y `develop`.

## 14. Licencia

Apache License 2.0. Ver [`LICENSE.txt`](LICENSE.txt).
