
-----

# Product Requirements Document: StreamNotifier (Phase 1)

**Project Name:** StreamNotifier
**Version:** 1.0.0
**Status:** Approved for Development
**Architecture:** Microservices (REST API)
**Tech Stack:** Java 21, Spring Boot 3.x, PostgreSQL, Docker

-----

## 1\. Executive Summary

StreamNotifier is a notification platform that monitors streaming services (YouTube, Twitch) and alerts users via their preferred communication channel (starting with Telegram).
**Phase 1 Goal:** Establish the **Core API Service** (Business Logic & DB) and the **Telegram Adapter Service** (User Interface), enabling users to register and manage subscriptions.

-----

## 2\. System Architecture

The system follows a **Microservices Architecture** pattern.

### 2.1. Services Overview

1.  **Core API Service (Java/Spring Boot):**
      * The "Brain" of the system.
      * Owns the database.
      * Manages `Accounts`, `Connections`, and `Subscriptions`.
      * Exposes REST API for adapters.
2.  **Telegram Adapter Service (Java or Python):**
      * The "Interface".
      * Stateless (No database).
      * Translates Telegram commands into REST calls to the Core API.
      * Receives internal signals from Core to send messages to users.

### 2.2. Communication Flow

  * **User Action:** User -\> Telegram Bot -\> Telegram Adapter -\> Core API.
  * **Notification:** External Event -\> Core API -\> Telegram Adapter -\> User.

-----

## 3\. Database Schema (Core Service Only)

The Core Service uses **PostgreSQL**.

### 3.1. Entity: `Account`

Represents a unique user in our system.

  * `id`: **UUID** (Primary Key).
  * `created_at`: **Timestamp**.

### 3.2. Entity: `Connection`

Links a specific platform identity to an Account.

  * `id`: **Long** (PK).
  * `account_id`: **UUID** (FK -\> Account).
  * `provider`: **ENUM** (`TELEGRAM`).
  * `provider_id`: **String** (e.g., Telegram Chat ID).
  * **Constraint:** Unique (`provider`, `provider_id`).

### 3.3. Entity: `Subscription`

Represents a monitored channel.

  * `id`: **Long** (PK).
  * `account_id`: **UUID** (FK -\> Account).
  * `platform`: **ENUM** (`YOUTUBE`, `TWITCH`).
  * `channel_url`: **String**.
  * `channel_name`: **String** (Optional, populated later).
  * **Constraint:** Unique (`account_id`, `channel_url`).

-----

## 4\. Functional Requirements

### 4.1. Core Service Features

  * **FR-CORE-01 (Auth):** Endpoint to register/retrieve a user based on Provider ID.
  * **FR-CORE-02 (Add Sub):** Endpoint to add a subscription. Must validate URL format.
  * **FR-CORE-03 (List Sub):** Endpoint to retrieve all subscriptions for a user.
  * **FR-CORE-04 (Remove Sub):** Endpoint to delete a subscription.
  * **FR-CORE-05 (Notify):** Logic to dispatch notifications to the correct Adapter.

### 4.2. Telegram Adapter Features

  * **FR-BOT-01 (Start):** Handle `/start`. Register user via Core.
  * **FR-BOT-02 (Add):** Handle `/add <url>`. Call Core to subscribe.
  * **FR-BOT-03 (List):** Handle `/list`. Show active subscriptions.
  * **FR-BOT-04 (Remove):** Handle `/remove <url>`. Call Core to unsubscribe.
  * **FR-BOT-05 (Push):** Expose internal endpoint to send messages triggered by Core.

-----

## 5\. API Contract (Interface Specification)

### 5.1. Authentication (Telegram -\> Core)

**POST** `/api/v1/users/auth`
*Request:*

```json
{
  "provider": "TELEGRAM",
  "providerId": "123456789",
  "username": "john_doe"
}
```

*Response (200 OK):*

```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "isNew": true
}
```

### 5.2. Manage Subscriptions (Telegram -\> Core)

**POST** `/api/v1/subscriptions`
*Request:*

```json
{
  "accountId": "550e8400-e29b-...",
  "url": "https://www.youtube.com/@MrBeast"
}
```

**GET** `/api/v1/subscriptions/{accountId}`
*Response (200 OK):*

```json
[
  {
    "id": 1,
    "platform": "YOUTUBE",
    "channelUrl": "https://www.youtube.com/@MrBeast"
  }
]
```

### 5.3. Internal Notification (Core -\> Telegram)

**POST** `http://telegram-service:8081/internal/send`
*Headers:* `X-Internal-Service-Key: <SECRET_ENV_KEY>`
*Request:*

```json
{
  "chatId": "123456789",
  "message": "🔴 **MrBeast** is live!\n\nLink: https://youtu.be/xyz"
}
```

-----

## 6\. Non-Functional Requirements (NFR)

### 6.1. Security

  * **Internal Security:** All internal service-to-service communication (Core \<-\> Telegram) must be secured via a Shared Secret Key passed in the header `X-Internal-Service-Key`.
  * **Input Validation:**
      * Core must validate URLs using Regex before saving.
      * YouTube Regex: `^https?://(www\.)?youtube\.com/@[\w-]+$`
      * Twitch Regex: `^https?://(www\.)?twitch\.tv/[\w-]+$`

### 6.2. Reliability & Error Handling

  * **Graceful Degradation:** If the Core Service is down, the Telegram Bot must reply with: *"System is temporarily unavailable, please try again later"* instead of crashing or silence.
  * **Standard Error Response:** All APIs must return errors in this format:
    ```json
    {
      "timestamp": "2023-10-27T10:00:00Z",
      "status": 400,
      "error": "Bad Request",
      "message": "Invalid URL format",
      "path": "/api/v1/subscriptions"
    }
    ```

### 6.3. Performance

  * API response time should not exceed **500ms**.
  * Database connections must be pooled (HikariCP).

-----

## 7\. Implementation Roadmap (Tasks for Agents)

### Phase 1.1: Core Service Foundation

1.  Initialize Spring Boot project (Web, JPA, Lombok, PostgreSQL).
2.  Create Domain Entities (`Account`, `Connection`, `Subscription`) with JPA constraints.
3.  Implement `GlobalExceptionHandler` to match the Standard Error Response JSON.

### Phase 1.2: Core Business Logic

4.  Implement `AuthController` (Registration logic).
5.  Implement `SubscriptionController` (Add/List/Remove) with Regex validation.

### Phase 1.3: Telegram Adapter

6.  Initialize Bot Project (Spring Boot + TelegramBots).
7.  Implement Command Handler Pattern (Strategy Pattern for commands `/start`, `/add`).
8.  Implement `RestClient` service to communicate with Core API.
9.  Implement `/internal/send` controller secured by API Key.

### Phase 1.4: DevOps

10. Create `docker-compose.yml` to run Postgres, Core, and Telegram Service in a single network.