# 🚚 Logistics & Supply Chain Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?logo=docker)](https://www.docker.com/)
[![OpenAPI](https://img.shields.io/badge/Swagger-OpenAPI%203.0-85EA2D.svg?logo=swagger)](http://localhost:8080/swagger-ui/index.html)
[![Actuator](https://img.shields.io/badge/Spring%20Actuator-Monitored-blueviolet.svg)](http://localhost:8080/actuator/health)

A modern, production-ready, enterprise-grade **Logistics & Supply Chain Management System** built with **Java 21** and **Spring Boot 4.1.1**. The platform provides end-to-end management of shipments, automated geocoding and distance-based tiered pricing, multi-warehouse routing, fleet and driver dispatching, real-time tracking timelines, last-mile delivery, digital Proof of Delivery (POD), delivery failure resolution, electronic payments and Cash on Delivery (COD) with Redis-powered idempotency, event-driven async notifications, and SMTP mailing.

---

## 📑 Table of Contents

- [System Architecture](#-system-architecture)
- [Core Features & Technical Highlights](#-core-features--technical-highlights)
- [Security & Role-Based Access Control (RBAC)](#-security--role-based-access-control-rbac)
- [Configuration & Environment Variables](#-configuration--environment-variables)
- [Docker & Deployment Guide](#-docker--deployment-guide)
- [API Reference & Response Specifications](#-api-reference--response-specifications)
  - [1. Authentication & Tokens (`/api/auth`)](#1-authentication--tokens)
  - [2. Customer Profiles (`/api/customers`)](#2-customer-profiles)
  - [3. Fleet & Vehicles (`/api/vehicles`)](#3-fleet--vehicles)
  - [4. Drivers & Personnel (`/api/drivers`)](#4-drivers--personnel)
  - [5. Branches & Hubs (`/api/branches`)](#5-branches--hubs)
  - [6. Warehouses & Facilities (`/api/warehouses`)](#6-warehouses--facilities)
  - [7. Shipment Lifecycle & Pricing Engine (`/api/shipments`)](#7-shipment-lifecycle--pricing-engine)
  - [8. Shipment Tracking & Chronological Timeline (`/api/shipments`)](#8-shipment-tracking--chronological-timeline)
  - [9. Warehouse Inbound/Outbound Movements (`/api/shipments`)](#9-warehouse-inboundoutbound-movements)
  - [10. Last-Mile Delivery Execution (`/api/deliveries`)](#10-last-mile-delivery-execution)
  - [11. Proof of Delivery (POD) (`/api/deliveries`)](#11-proof-of-delivery-pod)
  - [12. Delivery Failures & Incident Tracking (`/api/deliveries`)](#12-delivery-failures--incident-tracking)
  - [13. Delivery Rescheduling & Re-attempts (`/api/deliveries`)](#13-delivery-rescheduling--re-attempts)
  - [14. Electronic Payments & Idempotency (`/api/shipments`, `/api/payments`)](#14-electronic-payments--idempotency)
  - [15. Cash on Delivery (COD) (`/api/deliveries`, `/api/cod`)](#15-cash-on-delivery-cod)
  - [16. In-App Notifications & Alerts (`/api/notifications`)](#16-in-app-notifications--alerts)
- [Global Error Handling](#-global-error-handling)
- [Swagger UI & Observability](#-swagger-ui--observability)

---

## 🏛 System Architecture

The application adopts a decoupled, event-driven, domain-centric architecture:

```mermaid
flowchart TD
    Client([HTTP / Mobile / Web Clients]) -->|REST API + JWT Bearer| SecurityFilter[Spring Security & JWT Filter Chain]
    SecurityFilter --> RateLimiter[Redis Rate Limiter & Idempotency Check]
    RateLimiter --> Controllers[Spring MVC REST Controllers]

    subgraph Core Domain Services
        Controllers --> ShipmentSvc[Shipment & Pricing Service]
        Controllers --> DeliverySvc[Delivery & POD Service]
        Controllers --> PaymentSvc[Payment & COD Service]
        Controllers --> WarehouseSvc[Warehouse Movement Service]
        Controllers --> FleetSvc[Driver & Fleet Service]
    end

    ShipmentSvc --> GeoEngine[Geocoding & Distance Calculator - Nominatim/Haversine]
    PaymentSvc --> RedisIdempotency[Redis Idempotency Store TTL 24h]

    subgraph Event-Driven Subsystem
        ShipmentSvc -.->|Publish Event| AppEvents[Spring ApplicationEventPublisher]
        DeliverySvc -.->|Publish Event| AppEvents
        PaymentSvc -.->|Publish Event| AppEvents

        AppEvents -->|Async ThreadPool| NotifListener[Notification Event Listener]
        AppEvents -->|Async ThreadPool| EmailListener[Email Event Listener]

        NotifListener --> NotifSvc[Persistent Notification Service]
        EmailListener --> MailSender[JavaMailSender / SMTP Gateway]
    end

    Core Domain Services --> PostgreSQL[(PostgreSQL 17 Database)]
    NotifSvc --> PostgreSQL
    Core Domain Services --> RedisCache[(Redis 7 Cache)]
```

---

## 🌟 Core Features & Technical Highlights

1. **Automated Geocoding & Route Calculation**:
   - Converts textual pickup and delivery addresses to precise GPS coordinates via OpenStreetMap Nominatim.
   - Computes route distance using the **Haversine formula** with localized geocoding caching in Redis to prevent API rate-limiting.
2. **Tiered Dynamic Pricing Engine**:
   - Calculates shipping costs dynamically: `TotalPrice = BasePrice + (DistanceKm * RatePerKm) + (WeightKg * RatePerKg)`.
3. **Distributed Idempotency Protection**:
   - Critical operations (such as payment processing and cash collection) support the `Idempotency-Key` HTTP header.
   - Employs Redis atomic key reservation (`SETNX`) and SHA-256 payload hashing to prevent double charging and duplicate collections during network retries.
4. **Decoupled Event-Driven Async Architecture**:
   - Key domain transitions publish decoupled Spring Application Events (`ShipmentDeliveredEvent`, `PaymentPaidEvent`, `CodCollectedEvent`, `DeliveryFailedEvent`, `DeliveryRescheduledEvent`).
   - Processed asynchronously in a dedicated `ThreadPoolTaskExecutor` (10 core, 20 max threads) for non-blocking I/O.
5. **Multi-Channel Notifications**:
   - Simultaneously creates persistent in-app notifications in PostgreSQL and sends formatted HTML/text emails via SMTP.
6. **Chronological Tracking Audit Trail**:
   - Complete tracking history per shipment tracking number with real-time status transitions.

---

## 🔐 Security & Role-Based Access Control (RBAC)

The application enforces strict Method-Level Security (`@PreAuthorize`) based on JWT Bearer tokens with 4 distinct roles:

| Role | Description | Permissions |
|---|---|---|
| `ADMIN` | System Administrator | Full access to all entities, fleets, hubs, pricing, and administrative overrides. |
| `DISPATCHER` | Logistics Coordinator | Manage fleet, assign drivers/vehicles to shipments, route shipments across warehouses, reschedule deliveries. |
| `DRIVER` | Field Delivery Courier | View assigned deliveries, update delivery progress, submit Proof of Delivery (POD), collect COD payments, report failures. |
| `CUSTOMER` | Client / Shipper / Receiver | Create shipments, view own shipment timelines, pay online, view personal notifications. |

---

## ⚙️ Configuration & Environment Variables

All settings are environment-driven with production fallbacks defined in `application.properties`:

| Variable | Description | Default (Dev) | Production Required |
|---|---|---|:---:|
| `SPRING_PROFILES_ACTIVE` | Active profile (`dev` or `prod`) | `dev` | Yes |
| `SERVER_PORT` | HTTP port | `8080` | Optional |
| `DB_HOST` | PostgreSQL hostname | `localhost` | Yes |
| `DB_PORT` | PostgreSQL port | `5432` | Optional |
| `DB_NAME` | Database name | `logistics_db` | Yes |
| `DB_USERNAME` | Database username | `postgres` | Yes |
| `DB_PASSWORD` | Database password | `postgres` | Yes |
| `REDIS_HOST` | Redis host | `localhost` | Yes |
| `REDIS_PORT` | Redis port | `6379` | Optional |
| `REDIS_PASSWORD` | Redis authentication password | *(empty)* | Recommended |
| `JWT_SECRET` | 256-bit Base64 secret key | *(dev key)* | **Yes** |
| `JWT_ACCESS_EXPIRATION_MS` | Access token lifespan (ms) | `900000` (15m) | Optional |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token lifespan (ms) | `604800000` (7d) | Optional |
| `SPRING_MAIL_HOST` | SMTP server host | `smtp.mailtrap.io` | Yes |
| `SPRING_MAIL_PORT` | SMTP port | `587` | Optional |
| `SPRING_MAIL_USERNAME` | SMTP username | *(empty)* | Yes |
| `SPRING_MAIL_PASSWORD` | SMTP password | *(empty)* | Yes |
| `APP_GEOCODING_ENABLED` | Enable external Nominatim geocoding | `true` | Optional |

---

## 🐳 Docker & Deployment Guide

### Run Full Stack with Docker Compose
```bash
# Start PostgreSQL 17, Redis 7, and Logistics Spring Boot application
docker compose up -d
```

### Build & Run Container Manually
```bash
# 1. Package the executable jar
./mvnw clean package -DskipTests

# 2. Build Docker container image
docker build -t logistics-service:latest .

# 3. Run container
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgres \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=secret \
  -e REDIS_HOST=redis \
  --name logistics-app logistics-service:latest
```

---

## 📡 API Reference & Response Specifications

Every API response adheres to strict, typed DTO records. Below is the comprehensive guide to each domain module, its available endpoints, and the exact JSON response returned.

---

### 1. Authentication & Tokens

Manages user registration, credential authentication, and JWT access token rotation.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new user (`CUSTOMER`, `DRIVER`, `DISPATCHER`, `ADMIN`) | Public |
| `POST` | `/api/auth/login` | Authenticate user credentials and return JWT tokens | Public |
| `POST` | `/api/auth/refresh` | Issue a new Access Token using a valid Refresh Token | Public |

#### 📥 Sample Request (`POST /api/auth/login`)
```json
{
  "email": "ahmed.customer@example.com",
  "password": "Password123!"
}
```

#### 📤 Sample Response (`200 OK` / `201 Created`)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhaG1lZC5jdXN0b21lckBleGFtcGxlLmNvbSIsImF1dGhvcml0aWVzIjpbIlJPTEVfQ1VTVE9NRVIiXSwiaWF0IjoxNzg1NDIxNjAwLCJleHAiOjE3ODU0MjI1MDB9.s8f_xR...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhaG1lZC5jdXN0b21lckBleGFtcGxlLmNvbSIsImlhdCI6MTc4NTQyMTYwMCwiZXhwIjoxNzg2MDI2NDAwfQ.d7k_bM...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

---

### 2. Customer Profiles

Handles shipper and recipient client profiles, contact numbers, and default billing/pickup addresses.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/customers` | Create a customer profile linked to the authenticated user | `CUSTOMER`, `ADMIN` |
| `GET` | `/api/customers/me` | Retrieve the authenticated user's customer profile | Authenticated |
| `GET` | `/api/customers/{id}` | Retrieve customer profile by ID | Owner, `ADMIN` |
| `PUT` | `/api/customers/{id}` | Update customer profile details | Owner, `ADMIN` |
| `DELETE` | `/api/customers/{id}` | Delete customer profile | Owner, `ADMIN` |

#### 📤 Sample Response (`200 OK` / `201 Created`)
```json
{
  "id": 1,
  "userId": 10,
  "email": "ahmed.customer@example.com",
  "firstName": "Ahmed",
  "lastName": "Hassan",
  "phone": "+201012345678",
  "address": "15 El-Tahrir Square",
  "city": "Cairo",
  "postalCode": "11511"
}
```

---

### 3. Fleet & Vehicles

Tracks transportation assets, vehicle types, license plates, maximum payload capacities, and availability statuses (`AVAILABLE`, `IN_USE`, `MAINTENANCE`, `DECOMMISSIONED`).

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/vehicles` | Register a new fleet vehicle | `ADMIN` |
| `GET` | `/api/vehicles` | List all fleet vehicles | Authenticated |
| `GET` | `/api/vehicles/{id}` | Get vehicle by ID | Authenticated |
| `GET` | `/api/vehicles/plate/{plateNumber}` | Get vehicle by plate number | Authenticated |
| `PUT` | `/api/vehicles/{id}` | Update vehicle specifications and operational status | `ADMIN` |

#### 📤 Sample Response (`200 OK` / `201 Created`)
```json
{
  "id": 1,
  "plateNumber": "CAI-1234",
  "type": "VAN",
  "status": "AVAILABLE",
  "brand": "Mercedes-Benz",
  "model": "Sprinter 314",
  "manufacturingYear": 2023,
  "maxWeightKg": 1500.0
}
```

---

### 4. Drivers & Personnel

Manages courier personnel, driving licenses, validity dates, assigned vehicles, and real-time duty status (`AVAILABLE`, `ON_DUTY`, `BUSY`, `OFF_DUTY`).

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/drivers` | Register a driver profile | `ADMIN`, `DRIVER` |
| `GET` | `/api/drivers` | List all drivers | `ADMIN`, `DISPATCHER` |
| `GET` | `/api/drivers/me` | Get current driver's profile | Authenticated Driver |
| `GET` | `/api/drivers/{id}` | Get driver by ID | Owner, `ADMIN`, `DISPATCHER` |
| `PATCH` | `/api/drivers/{id}/status?status=AVAILABLE` | Update driver availability status | Owner, `ADMIN`, `DISPATCHER` |
| `PUT` | `/api/drivers/{id}` | Update driver profile and license details | Owner, `ADMIN` |
| `DELETE` | `/api/drivers/{id}` | Remove driver profile | Owner, `ADMIN` |

#### 📤 Sample Response (`200 OK` / `201 Created`)
```json
{
  "id": 1,
  "userId": 11,
  "email": "mohamed.driver@example.com",
  "firstName": "Mohamed",
  "lastName": "Mostafa",
  "phone": "+201098765432",
  "licenseNumber": "DL-EG-987654",
  "licenseExpiryDate": "2028-12-31",
  "status": "AVAILABLE"
}
```

---

### 5. Branches & Hubs

Regional distribution offices, dispatch headquarters, and operational sorting hubs.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/branches` | Create a new regional logistics branch | `ADMIN` |
| `GET` | `/api/branches` | List all active logistics branches | Authenticated |
| `GET` | `/api/branches/{id}` | Get branch by ID | Authenticated |
| `GET` | `/api/branches/code/{code}` | Get branch by unique alphanumeric code | Authenticated |
| `PUT` | `/api/branches/{id}` | Update branch contact info or address | `ADMIN` |

#### 📤 Sample Response (`200 OK` / `201 Created`)
```json
{
  "id": 1,
  "name": "Cairo Central Logistics Hub",
  "code": "CAI-HUB-01",
  "address": "Plot 42, Nasr Road, Nasr City",
  "city": "Cairo",
  "postalCode": "11765",
  "phone": "+20223456789",
  "email": "cairo.hub@logistics.com",
  "status": "ACTIVE"
}
```

---

### 6. Warehouses & Facilities

Physical fulfillment warehouses, sorting facilities, and intermediate storage centers.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/warehouses` | Register a new warehouse facility | `ADMIN` |
| `GET` | `/api/warehouses` | List all warehouses | Authenticated |
| `GET` | `/api/warehouses/{id}` | Get warehouse by ID | Authenticated |
| `GET` | `/api/warehouses/name/{name}` | Get warehouse by name | Authenticated |
| `PUT` | `/api/warehouses/{id}` | Update warehouse capacity and information | `ADMIN` |

#### 📤 Sample Response (`200 OK` / `201 Created`)
```json
{
  "id": 1,
  "name": "10th of Ramadan Main Fulfillment Center",
  "address": "Industrial Zone B3, Lot 12",
  "city": "Sharqia",
  "postalCode": "44629",
  "phone": "+201555123456",
  "email": "ramadan.wh@logistics.com",
  "status": "ACTIVE"
}
```

---

### 7. Shipment Lifecycle & Pricing Engine

The central engine for parcel intake, route distance computation, dynamic pricing calculations, driver/vehicle dispatch assignment, and status transitions (`CREATED`, `CONFIRMED`, `ASSIGNED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`).

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/shipments` | Create shipment with auto geocoding & pricing | Customer Owner, `ADMIN` |
| `GET` | `/api/shipments/{id}` | Get shipment by ID | Owner, Assigned Driver, Staff |
| `GET` | `/api/shipments/tracking/{trackingNumber}` | Get shipment details by tracking number | Owner, Assigned Driver, Staff |
| `GET` | `/api/shipments/customer/{customerId}` | List paginated customer shipments | Customer Owner, Staff |
| `PUT` | `/api/shipments/{id}` | Update package specs (recalculates pricing) | Customer Owner, Staff |
| `PATCH` | `/api/shipments/{id}/status` | Transition status according to finite state machine | `ADMIN`, `DISPATCHER` |
| `PATCH` | `/api/shipments/{shipmentId}/assignment` | Assign driver and vehicle to shipment | `ADMIN`, `DISPATCHER` |
| `DELETE` | `/api/shipments/{id}` | Cancel/delete unfulfilled shipment | Customer Owner, Staff |

#### 📥 Sample Request (`POST /api/shipments`)
```json
{
  "customerId": 1,
  "shipmentType": "STANDARD",
  "pickupAddress": "15 El-Tahrir Square",
  "pickupCity": "Cairo",
  "pickupPostalCode": "11511",
  "deliveryAddress": "Mansheya Square, Corniche Road",
  "deliveryCity": "Alexandria",
  "deliveryPostalCode": "21519",
  "recipientName": "Omar Farouk",
  "recipientPhone": "+201098765432",
  "packageDescription": "Fragile medical supplies and electronics",
  "weightKg": 12.5,
  "lengthCm": 40.0,
  "widthCm": 30.0,
  "heightCm": 25.0
}
```

#### 📤 Sample Response (`200 OK` / `201 Created`)
```json
{
  "id": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "customerId": 1,
  "customerName": "Ahmed Hassan",
  "customerEmail": "ahmed.customer@example.com",
  "status": "ASSIGNED",
  "shipmentType": "STANDARD",
  "pickupAddress": "15 El-Tahrir Square",
  "pickupCity": "Cairo",
  "pickupPostalCode": "11511",
  "deliveryAddress": "Mansheya Square, Corniche Road",
  "deliveryCity": "Alexandria",
  "deliveryPostalCode": "21519",
  "recipientName": "Omar Farouk",
  "recipientPhone": "+201098765432",
  "packageDescription": "Fragile medical supplies and electronics",
  "weightKg": 12.5,
  "lengthCm": 40.0,
  "widthCm": 30.0,
  "heightCm": 25.0,
  "distanceKm": 218.45,
  "basePrice": 50.00,
  "shippingFee": 327.68,
  "totalPrice": 377.68,
  "driver": {
    "id": 1,
    "userId": 11,
    "name": "Mohamed Mostafa",
    "phone": "+201098765432",
    "licenseNumber": "DL-EG-987654",
    "status": "AVAILABLE"
  },
  "vehicle": {
    "id": 1,
    "plateNumber": "CAI-1234",
    "type": "VAN",
    "status": "IN_USE"
  },
  "currentWarehouse": {
    "id": 1,
    "name": "10th of Ramadan Main Fulfillment Center",
    "code": "WH-CAI-01"
  },
  "createdAt": "2026-09-29T08:00:00",
  "updatedAt": "2026-09-29T08:30:00"
}
```

---

### 8. Shipment Tracking & Chronological Timeline

Provides public and private visibility into the chronological progress checkpoints of any shipment.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/shipments/{shipmentId}/tracking` | Get checkpoints (list or `?page=0&size=20`) | Owner, Assigned Driver, Staff |
| `GET` | `/api/shipments/track/{trackingNumber}` | Full timeline view with latest status | Owner, Assigned Driver, Staff |

#### 📤 Sample Response (`GET /api/shipments/track/SHP-20260929-ABCD1234`)
```json
{
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "currentStatus": "IN_TRANSIT",
  "shipment": {
    "id": 101,
    "trackingNumber": "SHP-20260929-ABCD1234",
    "status": "IN_TRANSIT",
    "recipientName": "Omar Farouk",
    "totalPrice": 377.68
  },
  "timeline": [
    {
      "id": 501,
      "shipmentId": 101,
      "status": "CREATED",
      "description": "Shipment registered in system and waiting for confirmation",
      "location": "Cairo Dispatch Hub",
      "createdAt": "2026-09-29T08:00:00"
    },
    {
      "id": 502,
      "shipmentId": 101,
      "status": "ASSIGNED",
      "description": "Assigned to driver Mohamed Mostafa (Van CAI-1234)",
      "location": "Cairo Central Logistics Hub",
      "createdAt": "2026-09-29T08:30:00"
    },
    {
      "id": 503,
      "shipmentId": 101,
      "status": "IN_TRANSIT",
      "description": "En route via Cairo-Alexandria Desert Highway",
      "location": "Cairo-Alexandria Highway Km 84",
      "createdAt": "2026-09-29T09:45:00"
    }
  ]
}
```

---

### 9. Warehouse Inbound/Outbound Movements

Records cross-docking, intermediate transfers, and storage relocation across fulfillment facilities.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `PATCH` | `/api/shipments/{shipmentId}/warehouse` | Move shipment to a target warehouse | `ADMIN`, `DISPATCHER` |
| `GET` | `/api/shipments/{shipmentId}/warehouse-movements` | List all historical warehouse movements | Owner, Staff |

#### 📥 Sample Request (`PATCH /api/shipments/101/warehouse`)
```json
{
  "warehouseId": 2,
  "notes": "Transferred for regional cross-docking before local courier dispatch"
}
```

#### 📤 Sample Response (`200 OK`)
```json
{
  "id": 201,
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "fromWarehouse": {
    "id": 1,
    "name": "10th of Ramadan Main Fulfillment Center"
  },
  "toWarehouse": {
    "id": 2,
    "name": "Alexandria West Logistics Sorting Center"
  },
  "movedAt": "2026-09-29T10:15:00",
  "notes": "Transferred for regional cross-docking before local courier dispatch"
}
```

---

### 10. Last-Mile Delivery Execution

Tracks active delivery attempts dispatched to drivers (`PENDING`, `IN_PROGRESS`, `DELIVERED`, `FAILED`, `CANCELLED`).

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/deliveries/{shipmentId}/start` | Start last-mile delivery run | `ADMIN`, `DISPATCHER`, `DRIVER` |
| `POST` | `/api/deliveries/{shipmentId}/complete` | Mark delivery completed | `ADMIN`, `DISPATCHER`, `DRIVER` |
| `GET` | `/api/deliveries/{deliveryId}` | Get delivery record by ID | Driver Owner, Customer, Staff |
| `GET` | `/api/deliveries/shipment/{shipmentId}` | Get delivery record by shipment ID | Driver Owner, Customer, Staff |

#### 📤 Sample Response (`200 OK`)
```json
{
  "id": 301,
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "driver": {
    "id": 1,
    "userId": 11,
    "name": "Mohamed Mostafa",
    "phone": "+201098765432",
    "licenseNumber": "DL-EG-987654",
    "status": "BUSY"
  },
  "status": "IN_PROGRESS",
  "startedAt": "2026-09-29T11:00:00",
  "deliveredAt": null,
  "deliveryNotes": "Driver dispatched on final delivery leg to Mansheya Square",
  "createdAt": "2026-09-29T11:00:00",
  "updatedAt": "2026-09-29T11:00:00"
}
```

---

### 11. Proof of Delivery (POD)

Digital delivery receipt capturing recipient identity, signature, photo evidence, and confirmation verification.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/deliveries/{deliveryId}/pod` | Create digital Proof of Delivery | Assigned Driver, Staff |
| `GET` | `/api/deliveries/{deliveryId}/pod` | Retrieve POD by delivery ID | Assigned Driver, Customer, Staff |
| `GET` | `/api/deliveries/shipment/{shipmentId}/pod` | Retrieve POD by shipment ID | Assigned Driver, Customer, Staff |

#### 📥 Sample Request (`POST /api/deliveries/301/pod`)
```json
{
  "recipientName": "Omar Farouk",
  "recipientPhone": "+201098765432",
  "recipientId": "NID-29508140102345",
  "notes": "Delivered to recipient personally at residential entrance"
}
```

#### 📤 Sample Response (`201 Created` / `200 OK`)
```json
{
  "id": 401,
  "deliveryId": 301,
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "recipientName": "Omar Farouk",
  "recipientPhone": "+201098765432",
  "recipientId": "NID-29508140102345",
  "notes": "Delivered to recipient personally at residential entrance",
  "confirmedAt": "2026-09-29T11:45:00",
  "createdAt": "2026-09-29T11:45:00"
}
```

---

### 12. Delivery Failures & Incident Tracking

Logs failed delivery attempts with structured reasons (`CUSTOMER_UNAVAILABLE`, `INCORRECT_ADDRESS`, `CUSTOMER_REJECTED`, `PACKAGE_DAMAGED`, `SECURITY_ACCESS_RESTRICTED`).

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/deliveries/{deliveryId}/failure` | Record a failed delivery attempt | Assigned Driver, Staff |
| `GET` | `/api/deliveries/{deliveryId}/failures` | Get failure history for a delivery | Driver, Customer, Staff |
| `GET` | `/api/deliveries/shipment/{shipmentId}/failures` | Get failure history for a shipment | Driver, Customer, Staff |

#### 📥 Sample Request (`POST /api/deliveries/301/failure`)
```json
{
  "reason": "CUSTOMER_UNAVAILABLE",
  "notes": "Doorbell unanswered and recipient phone was switched off after 3 attempts"
}
```

#### 📤 Sample Response (`200 OK`)
```json
{
  "id": 601,
  "deliveryId": 301,
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "reason": "CUSTOMER_UNAVAILABLE",
  "notes": "Doorbell unanswered and recipient phone was switched off after 3 attempts",
  "failedAt": "2026-09-29T12:00:00",
  "createdAt": "2026-09-29T12:00:00"
}
```

---

### 13. Delivery Rescheduling & Re-attempts

Reschedules uncompleted deliveries to future time slots and generates fresh delivery dispatch attempts.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/deliveries/{deliveryId}/reschedule` | Reschedule delivery with target date/time | Assigned Driver, Customer, Staff |
| `GET` | `/api/deliveries/reschedules/{rescheduleId}` | Get reschedule record by ID | Driver, Customer, Staff |
| `GET` | `/api/deliveries/shipment/{shipmentId}/reschedules` | List all reschedules for a shipment | Driver, Customer, Staff |
| `POST` | `/api/deliveries/shipment/{shipmentId}/new-attempt` | Generate a new delivery attempt dispatch | `ADMIN`, `DISPATCHER` |

#### 📥 Sample Request (`POST /api/deliveries/301/reschedule`)
```json
{
  "scheduledAt": "2026-09-30T14:30:00",
  "reason": "Recipient requested delivery tomorrow afternoon via SMS callback",
  "notes": "Requested to call 15 minutes before arrival"
}
```

#### 📤 Sample Response (`200 OK`)
```json
{
  "id": 701,
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "failedDeliveryId": 301,
  "scheduledAt": "2026-09-30T14:30:00",
  "reason": "Recipient requested delivery tomorrow afternoon via SMS callback",
  "notes": "Requested to call 15 minutes before arrival",
  "status": "SCHEDULED",
  "createdAt": "2026-09-29T12:30:00",
  "updatedAt": "2026-09-29T12:30:00"
}
```

---

### 14. Electronic Payments & Idempotency

Supports credit card, bank transfer, and digital wallet payments with **Redis Idempotency** protection via the `Idempotency-Key` HTTP header.

| Method | Endpoint | Description | Headers | Access |
|---|---|---|---|---|
| `POST` | `/api/shipments/{shipmentId}/payment` | Initiate online payment | `Idempotency-Key: <UUID>` | Owner, Staff |
| `GET` | `/api/payments/{paymentId}` | Get payment by payment ID | — | Owner, Staff |
| `GET` | `/api/shipments/{shipmentId}/payment` | Get payment by shipment ID | — | Owner, Staff |
| `PATCH` | `/api/payments/{paymentId}/pay` | Transition payment to `PAID` | — | `ADMIN`, `DISPATCHER` |
| `PATCH` | `/api/payments/{paymentId}/fail` | Record payment failure | — | `ADMIN`, `DISPATCHER` |
| `PATCH` | `/api/payments/{paymentId}/refund` | Process payment refund | — | `ADMIN`, `DISPATCHER` |
| `PATCH` | `/api/payments/{paymentId}/cancel` | Cancel pending payment | — | `ADMIN`, `DISPATCHER` |

#### 📥 Sample Request (`POST /api/shipments/101/payment`)
- **Header**: `Idempotency-Key: e82f1b0a-7c93-4e38-9cf8-12ab34cd56ef`
```json
{
  "method": "CREDIT_CARD",
  "amount": 377.68
}
```

#### 📤 Sample Response (`201 Created` / `200 OK`)
```json
{
  "id": 801,
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "amount": 377.68,
  "method": "CREDIT_CARD",
  "status": "PAID",
  "transactionReference": "TXN-20260929-982341",
  "paidAt": "2026-09-29T08:05:00",
  "refundedAt": null,
  "createdAt": "2026-09-29T08:00:00",
  "updatedAt": "2026-09-29T08:05:00"
}
```

---

### 15. Cash on Delivery (COD)

Physical cash collection upon delivery, courier reconciliation, and branch deposit tracking protected by `Idempotency-Key`.

| Method | Endpoint | Description | Headers | Access |
|---|---|---|---|---|
| `POST` | `/api/deliveries/{deliveryId}/cod` | Create COD tracking order | — | Driver, Customer, Staff |
| `GET` | `/api/cod/{codId}` | Get COD record by ID | — | Driver, Customer, Staff |
| `GET` | `/api/deliveries/{deliveryId}/cod` | Get COD record by delivery ID | — | Driver, Customer, Staff |
| `PATCH` | `/api/cod/{codId}/collect` | Driver records cash collection | `Idempotency-Key: <UUID>` | Assigned Driver, Staff |
| `PATCH` | `/api/cod/{codId}/fail` | Mark cash collection failed | — | Driver, Staff |
| `PATCH` | `/api/cod/{codId}/cancel` | Cancel COD order | — | Staff |

#### 📥 Sample Request (`PATCH /api/cod/901/collect`)
- **Header**: `Idempotency-Key: c94b2190-3245-4fd3-a641-71bc29f84321`
```json
{
  "collectedAmount": 377.68,
  "notes": "Full cash amount collected at customer doorstep; cash receipt issued"
}
```

#### 📤 Sample Response (`200 OK`)
```json
{
  "id": 901,
  "paymentId": 802,
  "shipmentId": 101,
  "trackingNumber": "SHP-20260929-ABCD1234",
  "deliveryId": 301,
  "driverId": 1,
  "amountToCollect": 377.68,
  "collectedAmount": 377.68,
  "status": "COLLECTED",
  "collectedAt": "2026-09-29T11:45:00",
  "collectedByDriverId": 1,
  "notes": "Full cash amount collected at customer doorstep; cash receipt issued",
  "createdAt": "2026-09-29T10:00:00",
  "updatedAt": "2026-09-29T11:45:00"
}
```

---

### 16. In-App Notifications & Alerts

Persisted PostgreSQL notifications generated automatically when domain events fire (such as `ShipmentDeliveredEvent`, `PaymentPaidEvent`, or `DeliveryFailedEvent`).

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/notifications` | Paginated notification history for current user | Authenticated |
| `GET` | `/api/notifications/unread` | Paginated unread notifications | Authenticated |
| `GET` | `/api/notifications/unread/count` | Total unread notification count | Authenticated |
| `PATCH` | `/api/notifications/{notificationId}/read` | Mark a specific notification as READ | Notification Owner |

#### 📤 Sample Response: Paginated Notifications (`GET /api/notifications`)
```json
{
  "content": [
    {
      "id": 1001,
      "userId": 10,
      "type": "SHIPMENT_DELIVERED",
      "title": "Shipment Delivered Successfully",
      "message": "Your shipment SHP-20260929-ABCD1234 has been successfully delivered to Omar Farouk.",
      "status": "UNREAD",
      "createdAt": "2026-09-29T11:45:01",
      "readAt": null
    },
    {
      "id": 1002,
      "userId": 10,
      "type": "PAYMENT_RECEIVED",
      "title": "Payment Confirmed",
      "message": "Payment of 377.68 EGP for shipment SHP-20260929-ABCD1234 was confirmed.",
      "status": "READ",
      "createdAt": "2026-09-29T08:05:01",
      "readAt": "2026-09-29T08:15:30"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0
}
```

#### 📤 Sample Response: Unread Count (`GET /api/notifications/unread/count`)
```json
{
  "unreadCount": 1,
  "count": 1
}
```

---

## 🚨 Global Error Handling

All controller errors and business exceptions are intercepted by `GlobalExceptionHandler` and returned in RFC-standard format:

### 1. Resource Not Found (`404 Not Found`)
```json
{
  "timestamp": "2026-09-29T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Shipment not found with id: 999"
}
```

### 2. Validation Failed (`400 Bad Request`)
```json
{
  "timestamp": "2026-09-29T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "weightKg": "Weight must be positive",
    "deliveryAddress": "Delivery address is required"
  }
}
```

### 3. Duplicate Idempotent Request Conflict (`409 Conflict`)
```json
{
  "timestamp": "2026-09-29T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "A concurrent request with Idempotency-Key 'c94b2190-3245-4fd3-a641-71bc29f84321' is already in progress. Please retry shortly."
}
```

### 4. Unauthorized / Access Denied (`401 Unauthorized` / `403 Forbidden`)
```json
{
  "timestamp": "2026-09-29T12:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied: You do not possess permission to access shipment 101."
}
```

---

## 📊 Swagger UI & Observability

### Interactive API Documentation (OpenAPI 3.0)
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Schema (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> [!TIP]
> Click the **Authorize** button in Swagger UI and provide your Bearer JWT token in the format: `Bearer <token>` to test secured endpoints directly in your browser.

### Production Health & Metrics (Spring Actuator)
- **Health Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) (inspects PostgreSQL, Redis, and disk space)
- **Application Info**: [http://localhost:8080/actuator/info](http://localhost:8080/actuator/info)
- **JVM & HTTP Metrics**: [http://localhost:8080/actuator/metrics](http://localhost:8080/actuator/metrics)

---

## 🛠 Local Development Quickstart

1. **Clone & Navigate**:
   ```bash
   git clone https://github.com/ahmedhassanasfour/logistics-management-system-spring-boot.git
   cd Logistics
   ```
2. **Launch Datastores via Docker Compose**:
   ```bash
   docker compose up -d postgres redis
   ```
3. **Run Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **Access Swagger UI**:
   Navigate to [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) to explore the system!

---

## 📄 License
This project is open-source and distributed under the [MIT License](LICENSE).
