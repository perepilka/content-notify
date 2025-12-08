# StreamNotifier

> A microservices-based notification platform that monitors streaming services (YouTube, Twitch) and delivers real-time alerts via Telegram.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?style=flat-square&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

**StreamNotifier** enables users to subscribe to their favorite content creators across multiple streaming platforms and receive instant notifications when they go live. The system is built with a modern microservices architecture, ensuring scalability, maintainability, and clear separation of concerns.

### Key Features

- ✅ **Multi-platform Support**: Monitor YouTube and Twitch channels
- ✅ **Telegram Integration**: Receive notifications directly in Telegram
- ✅ **RESTful API**: Clean, well-documented API for adapter services
- ✅ **Database Migrations**: Flyway-managed schema versioning
- ✅ **Production-Ready**: Comprehensive validation, error handling, and logging

---

## 🏗️ Architecture

StreamNotifier follows a **microservices architecture** pattern with clear separation between business logic and presentation layers.

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│   Telegram  │ ──────> │ Telegram Bot     │ ──────> │ Core Service│
│     User    │ <────── │   (Stateless)    │ <────── │ (REST API)  │
└─────────────┘         └──────────────────┘         └─────────────┘
                                                             │
                                                             ▼
                                                      ┌─────────────┐
                                                      │ PostgreSQL  │
                                                      │  Database   │
                                                      └─────────────┘
```

### Services

1. **Core Service** (`/core-service`)
   - Business logic and data persistence layer
   - RESTful API for managing accounts, connections, and subscriptions
   - PostgreSQL database with Flyway migrations
   - Built with Spring Boot 3.4 and Java 21

2. **Telegram Bot** (`/telegram-bot`)
   - Stateless interface layer for Telegram bot
   - Translates user commands to Core API calls
   - Built with Python 3.11+ and aiogram 3.x

3. **Infrastructure** (`/infrastructure`)
   - Docker Compose configurations
   - Environment variable templates
   - Deployment scripts

---

## 🛠️ Tech Stack

### Core Service (Backend)

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.0
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **Build Tool**: Maven
- **Key Libraries**:
  - Spring Data JPA (Data access)
  - Spring Validation (DTO validation)
  - Lombok (Boilerplate reduction)
  - SpringDoc OpenAPI (API documentation)
  - Testcontainers (Integration testing)

### Telegram Botls

- **Language**: Python 3.11+
- **Framework**: aiogram 3.x
- **HTTP Client**: aiohttp
- **Validation**: Pydantic

### DevOps

- **Containerization**: Docker & Docker Compose
- **Database**: PostgreSQL (Docker container)

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+** (or use included Maven wrapper)
- **Docker** & **Docker Compose**
- **Git**

### Quick Start

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/content-notify.git
   cd content-notify
   ```

2. **Set up environment variables**

   ```bash
   cd infrastructure
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start PostgreSQL database**

   ```bash
   docker-compose up -d
   ```

4. **Run the Core Service**

   ```bash
   cd ../core-service
   ./mvnw spring-boot:run
   ```

5. **Access the API documentation**

   Open your browser and navigate to:
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - API Docs: `http://localhost:8080/v3/api-docs`

---

## 📁 Project Structure

```
content-notify/
├── core-service/              # Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/perepilka/coreservice/
│   │   │   │   ├── domain/    # JPA Entities & Enums
│   │   │   │   ├── dto/       # Data Transfer Objects
│   │   │   │   ├── repository/# Spring Data Repositories
│   │   │   │   ├── service/   # Business Logic
│   │   │   │   └── controller/# REST Controllers
│   │   │   └── resources/
│   │   │       ├── db/migration/  # Flyway SQL migrations
│   │   │       └── application.properties
│   │   └── test/              # Unit & Integration tests
│   └── pom.xml
├── telegram-bot/          # Python Telegram Bot
├── infrastructure/            # Docker & deployment configs
│   ├── docker-compose.yml
│   ├── .env.example
│   └── .env
├── docs/                      # Documentation
│   ├── PRD-Phase1.md
│   └── TECH_STACK.md
├── .gitignore
└── README.md
```

---

## 🗄️ Database Schema

### Entity-Relationship Diagram

```
┌─────────────┐
│   Account   │
│─────────────│
│ id (UUID)   │◄──────┐
│ created_at  │       │
└─────────────┘       │
                      │
         ┌────────────┴──────────────┐
         │                           │
┌────────┴──────┐          ┌─────────┴──────┐
│  Connection   │          │  Subscription  │
│───────────────│          │────────────────│
│ id            │          │ id             │
│ account_id FK │          │ account_id FK  │
│ provider      │          │ platform       │
│ provider_id   │          │ channel_url    │
└───────────────┘          │ channel_name   │
                           └────────────────┘
```

### Tables

- **accounts**: User accounts in the system
- **connections**: Links between accounts and external platforms (Telegram)
- **subscriptions**: Content creator subscriptions per account

---

## 📚 API Documentation

Once the application is running, interactive API documentation is available via Swagger UI.

### Key Endpoints

| Method | Endpoint                          | Description                    |
|--------|-----------------------------------|--------------------------------|
| POST   | `/api/v1/users/auth`              | Register/authenticate user     |
| POST   | `/api/v1/subscriptions`           | Add new subscription           |
| GET    | `/api/v1/subscriptions/{accountId}`| List user subscriptions       |
| DELETE | `/api/v1/subscriptions/{id}`      | Remove subscription            |

For complete API specification, see the [PRD documentation](docs/PRD-Phase1.md).

---

## 💻 Development

### Running Locally

1. **Start PostgreSQL**:
   ```bash
   cd infrastructure && docker-compose up -d
   ```

2. **Run Core Service**:
   ```bash
   cd core-service
   ./mvnw spring-boot:run
   ```

3. **Run Tests**:
   ```bash
   ./mvnw test
   ```

4. **Build for Production**:
   ```bash
   ./mvnw clean package
   ```

### Code Style

- **Java**: Follow standard Spring Boot conventions
- **Lombok**: Use `@Data`, `@Builder`, `@RequiredArgsConstructor`
- **Dependency Injection**: Constructor injection only (no field injection)
- **No MapStruct**: Manual DTO mapping with `toDto()`/`toEntity()` methods

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`.

**Creating a new migration**:
```bash
# Format: V{version}__{description}.sql
# Example: V2__add_notification_settings.sql
```

---


## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ using Java 21 and Spring Boot 3**
