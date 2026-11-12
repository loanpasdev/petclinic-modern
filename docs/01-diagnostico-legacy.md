# 01 - Diagnóstico del código legacy (Spring PetClinic)

## 1. Objetivo

Registrar el estado del proyecto de partida antes de modificarlo: tecnologías, componentes,
funcionalidades, reglas de negocio, validaciones, deuda técnica y riesgos de migración.

Línea base analizada: rama `main`, commit `500158f` del repositorio
`https://github.com/spring-projects/spring-petclinic` (clon propio en `loanpas/petclinic-modern`,
historial preservado, 1047 commits).

## 2. Stack y versiones encontradas

| Componente | Versión | Observación |
|---|---|---|
| Java (compilación) | 17 | `java.version=17` en `pom.xml` |
| Spring Boot | 4.1.0 | `spring-boot-starter-parent` |
| Spring Framework | 7.0.8 | observado en runtime |
| Spring Web MVC + Thymeleaf | incluido | frontend server-side |
| Spring Data JPA / Hibernate | 7.4.1.Final | `ddl-auto=none` |
| Tomcat embebido | 11.0.22 | puerto 8080 |
| H2 | 2.4.240 | BD por defecto, en memoria |
| MySQL Connector/J | gestionado por Boot | perfil `mysql` |
| PostgreSQL Driver | gestionado por Boot | perfil `postgres` |
| Caffeine + Spring Cache | incluido | caché de veterinarios |
| Maven Wrapper | 3.9.16 | `./mvnw` |
| Gradle Wrapper | 9.5.1 | alternativa `./gradlew` |
| springdoc-openapi | **ausente** | hay que incorporarlo |
| Flyway / Liquibase | **ausente** | esquema por scripts SQL |

## 3. Estructura actual

```
org.springframework.samples.petclinic
├── PetClinicApplication          # main
├── PetClinicRuntimeHints         # hints para imagen nativa
├── model/                        # BaseEntity, NamedEntity, Person
├── owner/                        # Owner, Pet, PetType, Visit + repos + controladores MVC
├── vet/                          # Vet, Specialty, VetRepository, Vets, VetController
└── system/                       # WelcomeController, CrashController, WebConfiguration, CacheConfiguration
```

Acoplamiento actual: `owner` y `vet` comparten el paquete `model`; `Visit` vive dentro de `owner`
y referencia `Pet` por relación JPA. No existen límites de módulo ni reglas de dependencia.

## 4. Funcionalidades y endpoints actuales

| Operación | Ruta MVC | Método |
|---|---|---|
| Inicio | `/` | GET |
| Buscar propietarios | `/owners?lastName=&page=` | GET |
| Formulario de búsqueda | `/owners/find` | GET |
| Ver propietario | `/owners/{id}` | GET |
| Crear propietario | `/owners/new` | GET/POST |
| Editar propietario | `/owners/{id}/edit` | GET/POST |
| Añadir mascota | `/owners/{id}/pets/new` | GET/POST |
| Editar mascota | `/owners/{id}/pets/{petId}/edit` | GET/POST |
| Registrar visita | `/owners/{id}/pets/{petId}/visits/new` | GET/POST |
| Listar veterinarios | `/vets.html` y `/vets` (JSON) | GET |
| Página de error de demo | `/oups` | GET |

## 5. Modelo de datos

Tablas (scripts en `src/main/resources/db/h2|mysql|postgres`):

| Tabla | Columnas relevantes | Restricciones |
|---|---|---|
| `owners` | id, first_name, last_name, address, city, telephone | PK id; índice por `last_name` |
| `pets` | id, name, birth_date, type_id, owner_id | FK owner, FK type, **UNIQUE (owner_id, name)** |
| `types` | id, name | PK id |
| `visits` | id, pet_id, visit_date, description | FK pet |
| `vets` | id, first_name, last_name | PK id |
| `specialties` | id, name | PK id |
| `vet_specialties` | vet_id, specialty_id | FKs a vets y specialties |

Relaciones: `Owner 1-N Pet`, `Pet N-1 PetType`, `Pet 1-N Visit`, `Vet N-M Specialty`.

Datos de ejemplo (`data.sql`): 10 owners, 13 pets, 4 visits, 6 vets, 3 specialties, 6 types.

## 6. Validaciones y reglas de negocio

| Entidad | Regla | Implementación original |
|---|---|---|
| Person | `firstName` obligatorio, máx. 30 | `@NotBlank` + `@Size(max=30)` |
| Person | `lastName` obligatorio, máx. 30 | `@NotBlank` + `@Size(max=30)` |
| Owner | `address` obligatoria, máx. 255 | `@NotBlank` + `@Size(max=255)` |
| Owner | `city` obligatoria, máx. 80 | `@NotBlank` + `@Size(max=80)` |
| Owner | `telephone` obligatorio, exactamente 10 dígitos | `@Pattern(regexp="\\d{10}")` |
| Pet | `name` obligatorio, máx. 30 | `PetValidator` |
| Pet | `type` obligatorio al crear | `PetValidator` |
| Pet | `birthDate` obligatoria | `PetValidator` |
| Pet | nombre único por propietario | índice `unique_owner_pet_name` |
| Visit | `description` obligatoria, máx. 255 | `@NotBlank` + `@Size(max=255)` |
| Visit | `date` obligatoria; en alta por defecto mañana | `VisitController#getVisit` |
| Búsqueda | apellido "startsWith", sin espacios extremos | `OwnerController#processFindForm` |
| Búsqueda | página fuera de rango redirige a página 1 | `OwnerController` |
| Búsqueda | sin resultados muestra "not found" | `OwnerController` |

Reglas añadidas recientemente y que forman parte de la línea base:

- Mitigación de *integer overflow* en parámetros controlados por el usuario.
- Límites de longitud para evitar HTTP 500 por columnas excedidas.
- Nombres de mascota únicos por propietario.
- Normalización de espacios en la búsqueda de propietarios.

## 7. Persistencia y configuración

- Por defecto H2 **en memoria** (`jdbc:h2:mem:<uuid>`): los datos no sobreviven al reinicio.
- `spring.jpa.hibernate.ddl-auto=none`; el esquema lo crean `schema.sql`/`data.sql`.
- Perfiles `mysql` y `postgres` con variables de entorno (`MYSQL_*`, `POSTGRES_*`).
- `docker-compose.yml` provee MySQL 9.7 y PostgreSQL 18.4.
- La consola H2 **no está habilitada** en `application.properties` (falta
  `spring.h2.console.enabled=true`), aunque el README la menciona.
- `management.endpoints.web.exposure.include=*`: todos los endpoints de Actuator expuestos.
- Sin migraciones versionadas (no hay Flyway ni Liquibase).

## 8. Pruebas existentes

- Unitarias: `OwnerTests`, `VetTests`, `PetValidatorTests`, `PetTypeFormatterTests`, `ValidatorTests`.
- MVC: `OwnerControllerTests`, `PetControllerTests`, `VisitControllerTests`, `VetControllerTests`,
  `WelcomeControllerTests`, `CrashControllerTests`.
- Integración: `PetClinicIntegrationTests`, `MySqlIntegrationTests`, `PostgresIntegrationTests`,
  `CrashControllerIntegrationTests`, `I18nPropertiesSyncTest`, `PetClinicConcurrencyTests`.
- Utilidades: `EntityUtils`, `ClinicServiceTests`.

Riesgo: al retirar vistas MVC algunas de estas pruebas dejan de compilar; deben reemplazarse por
pruebas REST equivalentes o mantenerse mientras el módulo web siga existiendo.

## 9. Deuda técnica identificada

1. Paquete `model` compartido y entidades JPA usadas como contrato (sin DTO).
2. Lógica de presentación y de negocio mezclada en controladores MVC.
3. Sin API REST formal ni contrato OpenAPI.
4. Sin separación de módulos ni reglas de dependencia.
5. Esquema por scripts sin versionado de migraciones.
6. Caché de veterinarios sin política explícita de invalidación documentada.
7. Actuator completamente expuesto.
8. Consola H2 documentada pero no configurada.

## 10. Riesgos de migración

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Mover `Visit` a otro módulo rompe la relación JPA con `Pet` | Alto | `visits` depende de `owners` mediante puerto `PetLookup`; no mover la entidad a medias |
| Retirar Thymeleaf rompe pruebas existentes | Medio | Mantener el módulo web como adaptador mientras se añade la API |
| H2 `VARCHAR_IGNORECASE` no es portable | Medio | Resolver búsqueda case-insensitive en consulta (`lower(...)`) |
| Diferencias de esquema entre motores | Medio | Mantener los tres scripts y probar solo H2 en CI |
| Cambio de Java 17 a 21 | Bajo | Toolchain Java 21 y `maven.compiler.release=21` |

## 11. Elementos que se mantienen y que se reemplazan

**Se mantienen:** entidades y repositorios JPA, reglas de validación, datos de ejemplo, scripts SQL,
perfiles de BD, historial Git y licencia Apache 2.0.

**Se reemplazan:** controladores MVC como único acceso (pasan a adaptadores opcionales), entidades
como contrato público (se introducen DTO), ausencia de módulos y ausencia de OpenAPI.
