# Policy Service - Implementation Document

**Project:** policy-service
**Framework:** Spring Boot 4.1.1 (Java 21)
**Database:** PostgreSQL (`insureflow_policies`)
**Port:** 8081
**Date:** September 9, 2026

---

## 1. Inputs Provided by User

### 1.1 Objective
Build a fully functional **policy-service** as a standalone Spring Boot application:
- Design a normalised PostgreSQL schema for policyholders and policies
- Map Java classes to database tables using JPA/Hibernate annotations
- Implement a layered Spring Boot application: **Entity → Repository → Service → Controller**
- Expose a REST API with full CRUD and policy lifecycle operations (**issue, renew, cancel**)
- Enforce domain rules: policy effective dates, sum insured limits, premium calculations

### 1.2 Steps to Execute (User Requirements)

| # | Requirement |
|---|-------------|
| 1 | Bootstrap project at start.spring.io — Group: `com.insureflow`, Artifact: `policy-service`, Port: **8081** |
| 2 | Configure `application.properties`: DB `insureflow_policies`, JPA `ddl-auto=validate`, Flyway enabled |
| 3 | Write Flyway migrations: `V1__create_policyholder_table.sql`, `V2__create_policy_table.sql` |
| 4 | Create JPA Entities: `Policyholder.java` with `@OneToMany` → `Policy.java` |
| 5 | Repositories: `PolicyRepository` with custom finders — `findByPolicyholderId`, `findByPolicyNumber`, `findByStatusAndEndDateBefore` |
| 6 | DTOs: `CreatePolicyRequestDTO`, `PolicyResponseDTO`, `PolicyholderRequestDTO` |
| 7 | Service Layer: enforce domain rules — reject claims on EXPIRED/CANCELLED, validate `end_date > start_date`, calculate `annual_premium = sumInsured × productRateMap.get(productType)` |
| 8 | REST Controller at `/api/policies` with GET, POST, PUT (renew), DELETE (cancel) |
| 9 | Exception handlers: `PolicyNotFoundException`, `PolicyExpiredException`, `PolicyCancelledException` |

### 1.3 Database Table Details

#### Table 1: `policyholder`
| Column | Type / Constraint | Notes |
|--------|-------------------|-------|
| id | BIGINT, PK, Auto-Increment | Surrogate key |
| full_name | VARCHAR(200), NOT NULL | Legal name for policy documents |
| email | VARCHAR(255), NOT NULL, UNIQUE | Primary contact |
| phone | VARCHAR(20) | |
| date_of_birth | DATE, NOT NULL | Used in premium calculation (age factor) |
| address | TEXT | Insured location for property policies |
| created_at | TIMESTAMP, DEFAULT NOW() | |

#### Table 2: `policy`
| Column | Type / Constraint | Notes |
|--------|-------------------|-------|
| id | BIGINT, PK, Auto-Increment | |
| policy_number | VARCHAR(30), NOT NULL, UNIQUE | System-generated reference |
| product_type | VARCHAR(30), NOT NULL | MOTOR \| HOME \| TRAVEL \| HEALTH \| LIABILITY |
| sum_insured | DECIMAL(15,2), NOT NULL | Maximum payable on any single claim |
| annual_premium | DECIMAL(10,2), NOT NULL | Computed at issuance |
| status | VARCHAR(20), NOT NULL, DEFAULT 'ACTIVE' | ACTIVE \| EXPIRED \| CANCELLED \| LAPSED |
| start_date | DATE, NOT NULL | Coverage start |
| end_date | DATE, NOT NULL | Coverage end; must be after start_date |
| policyholder_id | BIGINT, FK → policyholder(id) | Owning customer |
| created_at | TIMESTAMP, DEFAULT NOW() | |
| cancelled_at | TIMESTAMP, NULLABLE | Set on cancellation |

### 1.4 User Choices / Confirmations
- **Product Rate Map** (agent-chosen, approved): MOTOR=0.05, HOME=0.03, TRAVEL=0.02, HEALTH=0.04, LIABILITY=0.06
- **Age Factor** (agent-chosen, approved): ≤30→1.0×, 31–45→1.1×, 46–60→1.3×, 61+→1.5×
- **Enums vs Strings:** Enums (`ProductType`, `PolicyStatus`) confirmed
- **PremiumCalculator:** kept as separate `@Component`
- **Execution mode:** Step by step, verify after each

---

## 2. Order-wise Steps Implemented

### ✅ Step 1 — pom.xml Dependencies Added
**File:** `pom.xml`

Added dependencies:
- `spring-boot-starter-data-jpa`
- `postgresql` (42.7.1, runtime)
- `flyway-core`
- `flyway-database-postgresql`
- `spring-boot-starter-validation`
- `lombok` (optional)
- `spring-boot-starter-test`

### ✅ Step 2 — application.properties Configured
**File:** `src/main/resources/application.properties`

```properties
spring.application.name=policy-service
server.port=8081

# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/insureflow_policies
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baselineOnMigrate=true
```

### ✅ Step 3 — Flyway Migration V1 (Policyholder Table)
**File:** `src/main/resources/db/migration/V1__create_policyholder_table.sql`
- Creates `policyholder` table with all 7 columns
- UNIQUE constraint on `email`
- Index on `email` for lookup performance

### ✅ Step 4 — Flyway Migration V2 (Policy Table)
**File:** `src/main/resources/db/migration/V2__create_policy_table.sql`
- Creates `policy` table with all 11 columns
- FK → `policyholder(id)` with `ON DELETE CASCADE`
- CHECK constraints: `end_date > start_date`, product_type enum, status enum, `sum_insured > 0`, `annual_premium >= 0`
- Indexes on `policyholder_id`, `policy_number`, `status`, `(status, end_date)`

### ✅ Step 5 — JPA Entities

| File | Description |
|------|-------------|
| `entity/ProductType.java` | Enum: MOTOR / HOME / TRAVEL / HEALTH / LIABILITY |
| `entity/PolicyStatus.java` | Enum: ACTIVE / EXPIRED / CANCELLED / LAPSED |
| `entity/Policyholder.java` | `@Entity` mapping to `policyholder` table; `@OneToMany` → Policy |
| `entity/Policy.java` | `@Entity` mapping to `policy` table; `@ManyToOne` → Policyholder; enums stored as STRING |

### ✅ Step 6 — Repositories

| File | Custom Finders |
|------|----------------|
| `repository/PolicyholderRepository.java` | `findByEmail`, `existsByEmail` |
| `repository/PolicyRepository.java` | `findByPolicyholderId`, `findByPolicyNumber`, `findByStatusAndEndDateBefore`, `existsByPolicyNumber` |

### ✅ Step 7 — DTOs

| File | Purpose |
|------|---------|
| `dto/PolicyholderRequestDTO.java` | Input for create/update policyholder (`@NotBlank`, `@Email`, `@Past`) |
| `dto/PolicyholderResponseDTO.java` | Response payload for policyholder |
| `dto/CreatePolicyRequestDTO.java` | Input for issuing a policy (`@NotNull`, `@DecimalMin`) |
| `dto/PolicyResponseDTO.java` | Response payload for policy (includes policyholder id & name) |

### ✅ Step 8 — Custom Exceptions

- `exception/PolicyNotFoundException.java`
- `exception/PolicyExpiredException.java`
- `exception/PolicyCancelledException.java`
- `exception/InvalidPolicyDataException.java`
- `exception/PolicyholderNotFoundException.java`

### ✅ Step 9 — PremiumCalculator (Utility Component)
**File:** `service/PremiumCalculator.java`

Formula:
```
annual_premium = sumInsured × productRateMap.get(productType) × ageFactor(dateOfBirth)
```

### ✅ Step 10 — Service Layer

| File | Responsibilities |
|------|------------------|
| `service/PolicyholderService.java` | CRUD operations, email uniqueness enforcement |
| `service/PolicyService.java` | `issue()`, `renew()`, `cancel()`, finders; enforces `end_date > start_date`; rejects operations on CANCELLED/EXPIRED policies; auto-generates `POL-XXXXXXXXXXXX` policy numbers |

### ✅ Step 11 — REST Controllers

**PolicyholderController** (`/api/policyholders`)
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create policyholder |
| GET | `/` | List all |
| GET | `/{id}` | Get by id |
| PUT | `/{id}` | Update |
| DELETE | `/{id}` | Delete |

**PolicyController** (`/api/policies`)
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Issue new policy |
| GET | `/` | List all |
| GET | `/{id}` | Get by id |
| GET | `/number/{policyNumber}` | Get by policy number |
| GET | `/policyholder/{policyholderId}` | List policies for a policyholder |
| PUT | `/{id}/renew` | Renew policy |
| DELETE | `/{id}` | Cancel policy |

### ✅ Step 12 — Global Exception Handler
**File:** `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`

| Exception | HTTP Status |
|-----------|-------------|
| `PolicyNotFoundException`, `PolicyholderNotFoundException` | **404 Not Found** |
| `PolicyExpiredException`, `PolicyCancelledException` | **409 Conflict** |
| `InvalidPolicyDataException`, `MethodArgumentNotValidException` | **400 Bad Request** |
| `Exception` (fallback) | **500 Internal Server Error** |

Standard error response body:
```json
{
  "timestamp": "2026-09-09T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Policy not found with id: 42"
}
```

---

## 3. Project Directory Structure

```
PolicyServiceApplication/
├── pom.xml
└── src/main/
    ├── java/com/PolicyServiceApplication/demo/
    │   ├── DemoApplication.java
    │   ├── controller/
    │   │   ├── PolicyController.java
    │   │   └── PolicyholderController.java
    │   ├── dto/
    │   │   ├── CreatePolicyRequestDTO.java
    │   │   ├── PolicyResponseDTO.java
    │   │   ├── PolicyholderRequestDTO.java
    │   │   └── PolicyholderResponseDTO.java
    │   ├── entity/
    │   │   ├── Policy.java
    │   │   ├── Policyholder.java
    │   │   ├── PolicyStatus.java
    │   │   └── ProductType.java
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   ├── InvalidPolicyDataException.java
    │   │   ├── PolicyCancelledException.java
    │   │   ├── PolicyExpiredException.java
    │   │   ├── PolicyNotFoundException.java
    │   │   └── PolicyholderNotFoundException.java
    │   ├── repository/
    │   │   ├── PolicyRepository.java
    │   │   └── PolicyholderRepository.java
    │   └── service/
    │       ├── PolicyService.java
    │       ├── PolicyholderService.java
    │       └── PremiumCalculator.java
    └── resources/
        ├── application.properties
        └── db/migration/
            ├── V1__create_policyholder_table.sql
            └── V2__create_policy_table.sql
```

---

## 4. Remaining Steps

1. Create PostgreSQL database `insureflow_policies` (must exist before app starts)
2. Run `mvn clean install` to verify full build
3. Start app via `mvn spring-boot:run` — Flyway auto-runs V1 & V2
4. Smoke-test endpoints with cURL / Postman
