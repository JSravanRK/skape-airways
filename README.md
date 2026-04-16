# Skape Airways — Airline Reservation System (Spring Boot + MySQL)

A full-stack **Airline Reservation System** built with **Spring Boot and MySQL** that supports flight search, seat selection, booking, payment, and a loyalty miles programme.

This project was developed as a **BTech Final Year Project** to demonstrate understanding of:
- Spring Boot REST API development
- JWT-based authentication & authorization
- JPA/Hibernate ORM with MySQL
- Dynamic pricing algorithms
- Frontend integration with vanilla HTML/CSS/JS
- Concurrency control with optimistic & pessimistic locking

---

## Features

- User registration and login with JWT authentication
- Search flights by origin, destination, and date
- Interactive seat map with real-time seat locking
- Dynamic pricing based on demand and time to departure
- Multi-passenger booking support
- Mock Razorpay payment gateway (Card, UPI, Net Banking)
- SkyMiles loyalty programme — earn and redeem miles
- Voucher system — Flight Discount & Seat Upgrade vouchers
- Email confirmations with e-ticket and boarding pass
- Admin dashboard with analytics, flight and user management
- Flight status tracking (public — no login required)

---

## Technologies Used

- Java 17
- Spring Boot 3.2
- Spring Security + JWT (JJWT 0.12.3)
- Spring Data JPA / Hibernate
- MySQL 8
- Maven
- HTML5 / CSS3 / Vanilla JavaScript
- Spring Mail (Gmail SMTP)

---

## Project Structure

```
airline-fixed
│
├── src/main/java/com/airline
│   ├── AirlineApplication.java
│   ├── config
│   │       DataSeeder.java
│   │       SecurityConfig.java
│   ├── controller
│   │       AuthController.java
│   │       FlightController.java
│   │       BookingController.java
│   │       PaymentController.java
│   │       MilesController.java
│   │       AdminController.java
│   │       AirportController.java
│   │       RazorpayMockController.java
│   ├── service
│   │       AuthService.java
│   │       FlightService.java
│   │       BookingService.java
│   │       PaymentService.java
│   │       MilesService.java
│   │       SeatService.java
│   │       AdminService.java
│   │       DynamicPricingService.java
│   │       EmailService.java
│   ├── model
│   │       User.java  Flight.java  Booking.java
│   │       Seat.java  Payment.java  Airport.java
│   │       MilesTransaction.java  Voucher.java
│   ├── repository
│   │       (JPA repositories for all entities)
│   ├── dto
│   │       AuthDto.java  FlightDto.java  BookingDto.java
│   │       PaymentDto.java  MilesDto.java  AnalyticsDto.java
│   ├── security
│   │       JwtUtil.java
│   │       JwtAuthFilter.java
│   ├── scheduler
│   │       SeatLockScheduler.java
│   └── enums
│           Role.java  FlightStatus.java  BookingStatus.java
│           PaymentStatus.java  SeatClass.java
│
├── src/main/resources
│   ├── application.properties
│   ├── sample-data.sql
│   └── static
│       ├── index.html
│       ├── login.html
│       ├── register.html
│       ├── css/style.css
│       ├── js/app.js
│       └── pages
│               search.html     book.html
│               bookings.html   confirmation.html
│               miles.html      status.html
│               admin/dashboard.html
│               admin/flights.html
│               admin/bookings.html
│               admin/users.html
│               admin/airports.html
│
└── pom.xml
```

---

## Database Setup

Open **MySQL Workbench** and run:

```sql
CREATE DATABASE airline_db;
```

> Tables are created automatically by Hibernate on first run (`ddl-auto=update`).  
> Default airports and admin account are seeded automatically on startup.

---

## Configure Database Connection

Update the credentials in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/airline_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

---

## Configure Email (Optional)

To enable real booking confirmation emails, update these properties:

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-gmail-app-password
email.enabled=true
```

> Get a Gmail App Password: Google Account → Security → 2-Step Verification → App Passwords

Leave `email.enabled=false` to run without email (bookings still work normally).

---

## Build and Run

Make sure **Java 17** and **Maven** are installed, then run in the project root:

```
mvn spring-boot:run
```

Then open your browser at:

```
http://localhost:8080
```

---

## Default Admin Credentials

```
Email    : skape@airways.com
Password : ska6air
```

---

## Application Pages

```
/                        → Home page
/login.html              → Sign in
/register.html           → Create account
/pages/search.html       → Search flights
/pages/book.html         → Select seat & pay
/pages/bookings.html     → My bookings
/pages/confirmation.html → E-ticket / boarding pass
/pages/miles.html        → SkyMiles dashboard
/pages/status.html       → Flight status tracker
/pages/admin/            → Admin panel (admin only)
```

---

## API Endpoints (Key)

```
POST   /api/auth/register          → Register
POST   /api/auth/login             → Login

GET    /api/flights/search         → Search flights
GET    /api/flights/status         → Flight status (public)
GET    /api/flights/{id}/seats     → Seat map

POST   /api/bookings               → Create booking
GET    /api/bookings/my            → My bookings
DELETE /api/bookings/{id}/cancel   → Cancel booking

POST   /api/payments/process       → Pay (Card/UPI)
POST   /api/payments/razorpay/confirm → Razorpay payment

GET    /api/miles/balance          → SkyMiles balance
POST   /api/miles/redeem           → Redeem miles for voucher

GET    /api/admin/analytics        → Dashboard stats (admin)
GET    /api/admin/users            → All users (admin)
```

---

## Example Flow

```
1. Register a new account at /register.html
2. Search for flights (e.g. DEL → BOM)
3. Select a seat on the interactive seat map
4. Fill in passenger details
5. Pay via Razorpay / Card / UPI
6. Download your e-ticket from the confirmation page
7. Earn SkyMiles automatically after payment
8. Redeem miles for vouchers on the SkyMiles page
9. Apply voucher codes at checkout for discounts or upgrades
```

---

## Learning Outcomes

- Built a production-grade REST API with Spring Boot and Spring Security
- Implemented JWT authentication with role-based access control
- Designed a concurrent seat locking system using pessimistic DB locks
- Applied dynamic pricing logic based on demand and time urgency
- Integrated a mock payment gateway simulating Razorpay's real API
- Developed a full-stack application with a responsive HTML/CSS/JS frontend
- Used optimistic locking (`@Version`) to prevent race conditions on flights and seats

---

## Author

**Vemuri Sravan Ram Kumar**  
B.Tech Final Year Student
