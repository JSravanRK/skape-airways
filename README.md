# ✈ Skape Airways — Airline Reservation System

**Spring Boot 3.2 · MySQL · Vanilla JS Frontend**

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x running locally

### 1. Create the database
```sql
CREATE DATABASE airline_db;
```

### 2. Configure credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=<your_mysql_password>
```

### 3. Run
```bash
mvn spring-boot:run
```
Open → http://localhost:8080

---

## Default Credentials

| Role  | Email               | Password  |
|-------|---------------------|-----------|
| Admin | skape@airways.com   | ska6air   |

> Admin and airports are auto-seeded on first startup.

---

## Feature Summary

- **Flight search** with real-time dynamic pricing (demand + urgency surcharges)
- **Seat selection** with 10-minute pessimistic locks (auto-released by scheduler)
- **Multi-passenger booking** in one session
- **Payment gateway** — Mock Razorpay modal + Card / UPI / Net Banking
- **SkyMiles rewards** — earn on every flight, redeem for vouchers
- **Voucher system** — FLIGHT_DISCOUNT (₹50 off) and SEAT_UPGRADE (Economy→Business)
- **Email notifications** — booking confirmation, payment receipt with boarding pass, cancellation
- **Admin dashboard** — analytics, flight management, user management, airport management
- **Fare calendar** — browse prices across 7 days
- **Flight status tracker**

---

## Bugs Fixed (v35 — this release)

### Backend

| # | File | Bug | Fix |
|---|------|-----|-----|
| 1 | `DataSeeder.java` | `existsByEmail("sravan@airline.com")` checked wrong email — admin was re-created on every restart, causing `Duplicate entry` crash | Changed to `existsByEmail("skape@airways.com")` |
| 2 | `AdminService.java` | `deleteUser()` only deleted payments + bookings, skipping `vouchers` and `miles_transactions` — caused FK constraint violation on delete | Added `voucherRepository.deleteAll(...)` and `milesTransactionRepository.deleteAll(...)` in correct FK order before deleting user |
| 3 | `AdminController.java` | `GET /api/admin/users` returned raw `User` JPA entities — exposed hashed passwords over the wire | Introduced `AdminDto.UserResponse` DTO; controller now maps entities to safe DTO before responding |
| 4 | `BookingDto.java` | `BookingResponse` had no `fromCode` / `toCode` fields — confirmation page couldn't show real IATA codes | Added `fromCode` and `toCode` to `BookingResponse` |
| 5 | `BookingService.java` | `toResponse()` never set `fromCode` / `toCode` | Added `r.setFromCode(...)` / `r.setToCode(...)` mapping airport codes |
| 6 | `MilesTransactionRepository.java` | Missing `@Repository` annotation | Added `@Repository` |
| 7 | `VoucherRepository.java` | Missing `@Repository` annotation | Added `@Repository` |
| 8 | `pom.xml` | Invalid `<n>` tag instead of `<name>` | Fixed to `<name>` |

### Frontend

| # | File | Bug | Fix |
|---|------|-----|-----|
| 9 | `pages/confirmation.html` | Airport codes shown as `first.fromCity.substring(0,3)` — "New Delhi" displayed as "NEW" instead of "DEL" | Changed to use `first.fromCode` from API response (with substring fallback) |
| 10 | `pages/book.html` | Deselecting a seat did not call the backend unlock API — seat stayed locked for 10 min, blocking other users | Added `await del(...)` to the release-lock endpoint in the deselect branch |

---

## Architecture

```
src/main/java/com/airline/
├── AirlineApplication.java
├── config/
│   ├── DataSeeder.java       — seeds admin + 12 airports on startup
│   └── SecurityConfig.java   — JWT stateless security
├── controller/               — REST endpoints
├── dto/                      — request/response shapes (no entity leaks)
├── enums/                    — BookingStatus, FlightStatus, SeatClass …
├── model/                    — JPA entities
├── repository/               — Spring Data JPA repositories
├── scheduler/
│   └── SeatLockScheduler.java — releases expired seat locks every 60s
├── security/
│   ├── JwtAuthFilter.java
│   └── JwtUtil.java
└── service/                  — business logic

src/main/resources/
├── application.properties
├── sample-data.sql           — optional sample flights
└── static/                   — frontend (HTML + CSS + JS)
```

---

## Email Setup

Email is enabled by default using the configured Gmail SMTP credentials in `application.properties`.  
To disable, set `email.enabled=false`.  
To use your own account, update `spring.mail.username` and `spring.mail.password` (App Password).

---

## Test Payment Values

| Method | Test Value | Result |
|--------|-----------|--------|
| Card   | Any card ending `0000` | Payment FAILED |
| UPI    | `fail@upi` | Payment FAILED |
| Razorpay mock | Any | ~95% SUCCESS |

