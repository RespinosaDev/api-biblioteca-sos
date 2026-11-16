# API REST · Gestión de biblioteca

API **REST** para gestionar los usuarios, libros y préstamos de una biblioteca, hecha con **Spring Boot 3**, **Spring Data JPA** y **PostgreSQL**. También pone sanciones automáticamente a quien devuelve un libro fuera de plazo.

Es una práctica de la asignatura *Sistemas Orientados a Servicios* (3.º del Grado en Ingeniería Informática, Universidad Politécnica de Madrid). La hice de forma individual.

---

## Tecnologías

| | |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.4 (Web, Data JPA, Validation) |
| Persistencia | PostgreSQL + Hibernate |
| Construcción | Maven (incluye wrapper `mvnw`) |
| Tests | JUnit 5 + MockMvc + H2 en memoria |
| Pruebas manuales | Postman |

## Arquitectura

Tiene la típica arquitectura por capas:

```
controller  →  service  →  repository  →  PostgreSQL
   (HTTP)      (lógica)     (Spring Data JPA)
```

```
src/main/java/com/biblioteca/
├── BibliotecaApplication.java      # Arranque + @EnableScheduling
├── config/SancionScheduler.java    # Tarea diaria que revisa préstamos vencidos
├── controller/                     # Endpoints REST
├── service/                        # Reglas de negocio y validaciones
├── repository/                     # Interfaces JpaRepository
├── model/                          # Entidades: Usuario, Libro, Prestamo
└── exception/                      # Manejador global de errores
```

### Modelo de datos

- **Usuario**: nombre de usuario, matrícula, fecha de nacimiento y email (los tres identificadores son únicos). También guarda si está sancionado.
- **Libro**: título, autor, edición, ISBN (único), editorial y disponibilidad.
- **Préstamo**: relaciona un usuario con un libro. Guarda la fecha de inicio y fin, si se ha devuelto y si ha generado sanción.

Un usuario y un libro tienen relación `1:N` con los préstamos.

## Endpoints

### Usuarios — `/api/usuarios`

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/usuarios` | Crear usuario (`201 Created` + cabecera `Location`) |
| `GET` | `/api/usuarios` | Listar usuarios |
| `GET` | `/api/usuarios/{id}` | Obtener un usuario |
| `PUT` | `/api/usuarios/{id}` | Actualizar un usuario |
| `DELETE` | `/api/usuarios/{id}` | Eliminar un usuario (`204`; solo si no tiene préstamos) |

### Libros — `/api/libros`

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/libros` | Crear libro |
| `GET` | `/api/libros?titulo=&disponible=&page=&size=` | Buscar libros con filtros y **paginación** |
| `GET` | `/api/libros/{id}` | Obtener un libro |
| `PUT` | `/api/libros/{id}` | Actualizar un libro |
| `DELETE` | `/api/libros/{id}` | Eliminar un libro |

### Préstamos — `/api/prestamos`

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/prestamos?usuarioId=&libroId=` | Prestar un libro (2 semanas) |
| `PUT` | `/api/prestamos/{id}/devolver` | Devolver un libro |
| `PUT` | `/api/prestamos/{id}/ampliar?dias=7` | Ampliar el plazo de un préstamo |
| `GET` | `/api/prestamos/{id}` | Obtener un préstamo |
| `GET` | `/api/prestamos/usuario/{usuarioId}/activos` | Préstamos activos de un usuario (paginado) |
| `GET` | `/api/prestamos/usuario/{usuarioId}/historial` | Préstamos ya devueltos de un usuario (paginado) |

## Reglas de negocio

- Un préstamo dura **2 semanas** y se puede ampliar mientras no haya vencido ni se haya devuelto.
- Un libro no se puede prestar si no está disponible o ya tiene un préstamo activo.
- Un **usuario sancionado** no puede pedir préstamos.
- **Sanciones automáticas**: cada día a medianoche (`@Scheduled(cron = "0 0 0 * * ?")`) se buscan los préstamos vencidos sin devolver. Se marcan como sancionados, igual que su usuario. Devolver un libro fuera de plazo también marca el préstamo como sancionado.
- No se pueden borrar usuarios ni libros que tengan préstamos asociados.

## Errores

Los errores devuelven un JSON con `timestamp`, `status`, `error` y `message`:

| Código | Cuándo |
|--------|--------|
| `400 Bad Request` | Datos inválidos, JSON mal formado o falta un parámetro obligatorio |
| `404 Not Found` | El usuario, libro o préstamo no existe |
| `409 Conflict` | Duplicados (ISBN, email…), libro ya prestado, usuario sancionado o préstamo ya devuelto |
| `500 Internal Server Error` | Error inesperado |

## Ejemplo de uso

```bash
# Crear un usuario
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"ana_g","matricula":"A001","fechaNacimiento":"2002-05-14","email":"ana@example.com"}'

# Crear un libro
curl -X POST http://localhost:8080/api/libros \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Dune","autor":"Frank Herbert","isbn":"9780441013593","editorial":"Ace"}'

# Prestar el libro 1 al usuario 1
curl -X POST "http://localhost:8080/api/prestamos?usuarioId=1&libroId=1"
```

## Puesta en marcha

### 1. Base de datos

```sql
CREATE DATABASE biblioteca_db;
\c biblioteca_db
CREATE SCHEMA biblioteca_schema;
```

Las tablas las crea Hibernate al arrancar (`ddl-auto=update`).

### 2. Credenciales

La conexión se configura con variables de entorno. Si no se definen, se usan valores por defecto:

| Variable | Por defecto |
|----------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/biblioteca_db` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |

### 3. Ejecutar

```bash
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows
```

La API queda disponible en `http://localhost:8080`.

### 4. Tests

```bash
./mvnw test
```

Los tests usan una base de datos **H2 en memoria** (perfil `test`), así que no necesitan PostgreSQL. Cubren el flujo completo de un préstamo, las sanciones y los códigos de error.

## Cosas que me gustaría mejorar

- Usar DTOs y Bean Validation (`@Valid`, `@NotBlank`…) en lugar de validar a mano en los servicios.
- Añadir un mecanismo para levantar sanciones.
- Tests de integración contra PostgreSQL real con Testcontainers.
- Documentar la API con OpenAPI/Swagger.
