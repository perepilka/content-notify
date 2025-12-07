# AGENTS.md - Project Knowledge Base

## 🗺️ Project Map
This repository contains a microservices-based notification system.

### 📂 /core-service (The Brain)
- **Role:** Central API, Database Owner, User Management.
- **Tech:** Java 21, Spring Boot 3.2, PostgreSQL.
- **Key Locations:**
  - `src/main/resources/db/migration` -> Flyway SQL scripts.
  - `src/main/java/com/perepilka/coreservice/web` -> REST Controllers.
  - `src/main/java/com/perepilka/coreservice/domain` -> JPA Entities.

### 📂 /telegram-bot (The Interface)
- **Role:** User Interface via Telegram. Stateless adapter.
- **Tech:** Python 3.11, Aiogram 3, Aiohttp.
- **Key Locations:**
  - `src/handlers` -> Command processing (/start, /add).
  - `src/services` -> HTTP Client for Core API.

### 📂 /infrastructure (The Environment)
- **Role:** Local development setup.
- **Files:** `docker-compose.yml`, `.env` template.

## 🔄 Workflow for Agents
When I ask you to implement a feature:
1. **Identify the Service:** Are we in Core (Java) or Bot (Python)?
2. **Check the Contract:** Look at `docs/PRD-Phase1.md` API section.
3. **Check the Stack:** Look at `docs/TECH_STACK.md`.
4. **Implementation:** Write the code.
5. **Verification:** Suggest how to test this (e.g., "Run docker-compose up and send curl...").