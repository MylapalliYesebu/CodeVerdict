# CodeVerdict

![CI](https://github.com/MylapalliYesebu/CodeVerdict/actions/workflows/ci.yml/badge.svg)

**A highly-performant, secure, and asynchronous backend for evaluating user-submitted algorithms.**

CodeVerdict is a custom-built online judge system entirely decoupled from heavy web frameworks. It evaluates arbitrary Java code against hidden test cases in an isolated execution sandbox, utilizing a lightweight connection-pooling layer, robust JWT authentication, LRU service-caching, and an active queue system.

---

## 🚀 Features

- **Asynchronous Execution Queue**: Java submissions are compiled and judged concurrently via a bounded thread-pool (`ExecutorService`). 
- **Sandboxed Compilation**: Secure isolation evaluating source code using subprocesses (`ProcessBuilder`), with strict compile-time and execution-time bounds avoiding DoS/infinite loop abuse.
- **LRU In-Memory Caching**: Heavy leaderboard queries and problem listings are cached globally via a custom Thread-Safe `SimpleCache` (ReadWriteLock architecture).
- **Hardened Security**: Custom IP-based Rate Limiter mapped across `ConcurrentHashMap`, and robust bcrypt JWT Session tracking.
- **Dynamic Leaderboard**: Fast `GROUP BY` aggregations scoring user acceptance ratios asynchronously.
- **Zero-Dependency Core Routing**: Runs on standard `com.sun.net.httpserver`, minimizing bootstrap latency and container size.

---

## 🛠️ Tech Stack

- **Core**: Java 17
- **Routing & Networking**: `com.sun.net.httpserver.HttpServer`
- **Database**: PostgreSQL (JDBC) with custom Thread-Safe connection pooling
- **Security**: `BCrypt`, JWT-like custom token sessions
- **JSON Serialization**: `Gson`
- **Build & Management**: Maven (`pom.xml`)

---

## 📂 Architecture Overview

```text
src/main/java/com/codeverdict/
├── auth/         # BCrypt hashing, JWT generation, session validations
├── database/     # Abstracted DAOs, Schema DDL, and Connection Pools
├── handlers/     # Core REST API endpoints (Problems, Submissions, Leaderboards)
├── judge/        # The Execution Engine (Compilers, Sandboxes, Verdicts)
├── models/       # Database entities (User, Problem, TestCase, Submission)
├── server/       # HTTP Server Bootstrapper and Request Logging Filters
└── utils/        # LRU Caches, Env Configs, Rate Limiters, Exception Mappers
```

---

## 📦 Setup & Installation

**1. Clone the Repository**
```bash
git clone https://github.com/MylapalliYesebu/codeverdict.git
cd codeverdict
```

**2. Configure the Environment**
CodeVerdict dynamically loads an `.env` file from the root directory during startup.
```bash
cp .env.example .env
```
Fill out the variables inside `.env` with your local PostgreSQL database credentials.

**3. Build the Application**
Ensure you have Maven and Java 17+ installed.
```bash
mvn clean package -DskipTests
```
This generates a shaded uber-jar containing all core dependencies.

**4. Start the Server**
```bash
java -jar target/codeverdict.jar
```
*Note: The schema will automatically migrate and synchronize upon boot.*

---

## 🌍 API Endpoints

| Method | Endpoint               | Description                                   | Auth Required |
|--------|------------------------|-----------------------------------------------|---------------|
| `GET`  | `/api/health`          | Fetch live DB Pool & Judge Queue telemetry.   | ❌            |
| `POST` | `/api/auth/signup`     | Register a new account.                       | ❌            |
| `POST` | `/api/auth/login`      | Authenticate and receive a JWT token.         | ❌            |
| `POST` | `/api/auth/logout`     | Destroy current session token.                | ✅            |
| `GET`  | `/api/problems`        | Retrieve paginated problem listings.          | ❌            |
| `POST` | `/api/problems`        | Admin endpoint to create new questions.       | ✅ (ADMIN)    |
| `POST` | `/api/submit`          | Submit Java code for asynchronous execution.  | ✅            |
| `GET`  | `/api/submissions/:id` | Fetch the final verdict of a submission.      | ✅            |
| `GET`  | `/api/leaderboard`     | Fetch the globally aggregated leaderboard.    | ❌            |

---

## 🧪 Testing

CodeVerdict has 100% core business-logic coverage mapped across `JUnit 5`.
```bash
# Run the unit test suite
mvn clean test
```
To run the end-to-end API integration flow locally against a live database:
```bash
./test-api-workflow.sh
```

---

## ☁️ Deployment

CodeVerdict is fully infrastructure-as-code ready for [Render](https://render.com). 
A `render.yaml` configuration is located in the root repository mapping the Java environment bounds.

Just hook the repository to Render, configure your `.env` variables in the dashboard, and the pipeline will build automatically via `mvn clean package`.

---

## 📜 License
*Not Yet Licensed* - Please attach an appropriate Open-Source License (e.g. MIT, Apache 2.0) before distributing.