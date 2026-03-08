# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot REST API for a liquor shop inventory and billing system. Manages products, batch-level stock, purchases (stock in), sales (stock out with FIFO allocation), and JWT-based authentication.

## Build & Run Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Run tests
./gradlew test


# Run a single test class
./gradlew test --tests "com.liquorshop.inventory.SomeTest"

# Skip tests during build
./gradlew build -x test
```

## Required Configuration

`application.properties` only sets `spring.application.name`. The following must be provided (e.g., via `application-local.properties` or environment variables):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<db>
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=validate
spring.sql.init.mode=always

jwt.secret=<min-32-char-secret>
jwt.expiration=86400000
jwt.refresh-expiration=604800000
```

The schema is defined in `src/main/resources/schema.sql` and is PostgreSQL-specific.

## Architecture

**Stack:** Spring Boot 4.0.2, Java 21, PostgreSQL, JWT (jjwt 0.12.5), Lombok, MapStruct 1.6.3.

**Package layout:**
- `controller/` — REST controllers (thin, delegate to services)
- `service/` — Business logic
- `entity/` — JPA entities (suffix `Entity`)
- `dto/` — Request/response objects
- `repository/` — Spring Data JPA repositories
- `security/` — JWT filter, token provider, `CustomUserDetailsService`
- `mapper/` — MapStruct interfaces for entity↔DTO mapping
- `exception/` — `GlobalExceptionHandler` + custom exceptions

**Authentication flow:**
- `POST /api/auth/register` and `/api/auth/login` return an access token (short-lived JWT) and a refresh token (stored in `refresh_tokens` table).
- `POST /api/auth/refresh` rotates the refresh token (old one revoked, new one issued).
- All `/api/**` routes except `/api/auth/**` require `Bearer` token.

**Core domain model:**
- `ProductEntity` — SKU master (name, brand, category, volume_ml, barcode, min_stock).
- `BatchEntity` — A stock batch tied to a product with its own purchase/selling price, current quantity, and optional expiry date. Created automatically on each purchase line.
- `PurchaseEntity` / `PurchaseLineEntity` — A purchase invoice with line items, each of which creates a new `BatchEntity` and increments stock.
- `SaleEntity` / `SaleLineEntity` — A sale with FIFO batch allocation: batches with expiry dates are consumed first (earliest expiry first), then batches without expiry dates, ordered by `created_at`.
- `ProductBatchEntity` — A separate, newer entity (with `Quantity` enum, supplier name, total quantity) currently under development alongside the older `BatchEntity`. Uses MapStruct via `ProductBatchMapper`.

**Stock control:**
- Stock quantity lives on `BatchEntity.currentQuantity`. Sales decrease it; purchases create new batches.
- `InventoryService` aggregates across all batches per product to compute total quantity, stock value, low-stock flags, and expiring batches.

**Important schema note:** `schema.sql` uses table names `productEntities` and `batchEntities`, but the JPA `@Table` annotations on `ProductEntity` and `BatchEntity` map to `products` and `batches`. Ensure these are reconciled if running `schema.sql` directly against a new database.
