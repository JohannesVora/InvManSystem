# Inventory Management System

A multi-module Java Spring Boot inventory management system with a React frontend.

## Architecture

```
inventory-system/          ← Maven parent
├── common/                ← Shared entities, repos, DTOs, connector contracts
├── data-processing-module/← Business logic, REST API, services
├── inbound-connector-module/ ← POS / webhook inbound event handling
├── outbound-connector-module/ ← Supplier order transmission (email, REST, EDI)
├── app/                   ← Spring Boot assembly, Flyway migrations
└── frontend/              ← React 18 + Vite single-page app
```

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for containerized run)
- Node.js 20+ (for frontend dev)

### Run with Docker Compose

```bash
cp .env.example .env
# Edit .env with your SMTP credentials
docker-compose up --build
```

- API: http://localhost:8080/api/health
- Frontend: http://localhost:80

### Run Locally (Development)

```bash
# Build everything
mvn clean package -DskipTests

# Run the API
java -jar app/target/app-1.0.0-SNAPSHOT.jar

# Run frontend dev server
cd frontend
npm install
npm run dev
```

### Run Tests

```bash
mvn test
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/health | Health check |
| GET | /api/inventory | List all inventory items |
| POST | /api/orders/replenishment | Create replenishment order |
| GET | /api/orders/replenishment/{id} | Get order status |
| GET | /api/suppliers | List suppliers |
| GET | /api/suppliers/{id}/connector | Get connector config |
| PUT | /api/suppliers/{id}/connector | Update connector config |
| GET | /api/settings/smtp | Get SMTP settings |
| PUT | /api/settings/smtp | Update SMTP settings |

## Database

SQLite database stored at `./data/inventory.db` (configurable via `app/src/main/resources/application.yml`).

Flyway migrations in `app/src/main/resources/db/migration/`:
- `V1__init.sql` — full schema
- `V2__test_data.sql` — seed data
- `V3__app_settings.sql` — app settings table
