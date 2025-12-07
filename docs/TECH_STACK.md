# Technical Stack & Coding Standards

## 1. Global Architecture (All Services)
* **Architecture:** Microservices via REST API (JSON).
* **Communication:**
    * Services communicate via HTTP (internal network).
    * Use specific DTOs for Request/Response.
* **Deployment:** Docker Compose (each service has its own `Dockerfile`).
* **Secrets:** All sensitive data (Tokens, Passwords) must be loaded from `.env` file.

---

## 2. Core Service (Backend)
**Directory:** `/core-service`

### 2.1. Core Technologies
* **Language:** Java 21.
* **Framework:** Spring Boot 3.2+ (Web, Data JPA, Validation).
* **Build Tool:** Maven.
* **Database:** PostgreSQL.
* **Migration:** Flyway (SQL-based migrations).

### 2.2. Java Rules (ALLOWED)
* **Lombok:** REQUIRED for POJOs (`@Data`, `@Builder`, `@RequiredArgsConstructor`).
* **Swagger/OpenAPI:** `springdoc-openapi` for API docs.
* **Testing:** JUnit 5, Mockito. Testcontainers (optional for integration).

### 2.3. Java Rules (FORBIDDEN)
* ❌ **MapStruct:** DO NOT USE. Use manual mapping (`toDto`/`toEntity` methods).
* ❌ **Jib:** DO NOT USE. Standard `Dockerfile` only.
* ❌ **H2:** DO NOT USE. Postgres only.

### 2.4. Java Coding Conventions
* **Injection:** Constructor Injection only. No `@Autowired` on fields.
* **Error Handling:** Use `@ControllerAdvice` + Global Exception Handler.

---

## 3. Telegram Adapter (Bot)
**Directory:** `/telegram-bot`

### 3.1. Core Technologies
* **Language:** Python 3.11+.
* **Framework:** `aiogram 3.x` (Asynchronous Telegram Framework).
* **Package Manager:** `poetry` (Standard for dependency management).
* **HTTP Client:** `aiohttp` (For making async requests to Core Service).

### 3.2. Python Rules (ALLOWED)
* **Pydantic:** REQUIRED for data validation and parsing JSON from Core Service.
* **Typing:** Strict type hinting is REQUIRED (`def func(a: int) -> str:`).
* **Linting:** `ruff` (for linting and formatting).
* **Env:** `pydantic-settings` for managing environment variables.

### 3.3. Python Rules (FORBIDDEN)
* ❌ **Synchronous code:** DO NOT USE `requests` or `time.sleep()`. Only `async/await`.
* ❌ **Global Variables:** Avoid global state.
* ❌ **Direct Database Access:** The bot MUST NOT connect to PostgreSQL directly. It must use Core API.

### 3.4. Python Coding Conventions
* **Structure:** Handlers, Keyboards, and Middlewares must be in separate packages.
* **FSM:** Use `aiogram` FSMContext for state management (e.g. waiting for user input).