# I-ITSM — Intelligent IT Service Management & Incident Resolution System

CS F213 / MAC F212 Object-Oriented Programming project. This is a full-stack incident management platform that takes an incident through every stage: **report, classify, prioritise, assign, investigate, resolve, verify, close**. Along the way it manages SLAs, sends notifications, keeps an audit trail, and offers analytics and intelligent automation.

**Live demo:** https://itsm-273t.onrender.com. Sign in with any demo account below. On the free tier, the first visit after a period of inactivity takes about a minute to wake the service.

| | |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (MVC, Security, Data JPA, WebSocket/STOMP, Scheduling), JJWT, springdoc |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4, Radix UI, TanStack Query, Recharts |
| Data | PostgreSQL (production), H2 (development and tests) |
| DevOps | Docker multi-stage image, Docker Compose, GitHub Actions CI, Render blueprint |

## Highlights

- **OOP design.** The domain model has a `User` hierarchy (`TeamLead extends Technician`, `ITManager extends Administrator`). `Incident` is an encapsulated aggregate whose notes, SLA tracker, resolution and feedback are composed into it.
- **8 design patterns:**
  - State: the incident lifecycle, with 10 state classes
  - Strategy: 2 priority algorithms and 4 assignment algorithms, switchable at runtime
  - Observer: audit, notification and live-dashboard listeners
  - Factory: users, incidents, channels and states
  - Adapter: SMTP and SMS gateway
  - Repository
  - Template Method
  - Flyweight
- **6 intelligent features:**
  - auto-categorisation (keywords plus learning from history)
  - TF-IDF duplicate detection
  - correlation through the service dependency graph, giving root-cause hints and major-incident detection
  - predictive SLA risk
  - four-tier automatic escalation
  - smart technician recommendation and suggested resolutions
- **Real-time.** The operations dashboard and per-user notifications update over WebSocket.
- **Security.** JWT authentication, BCrypt password hashing, role-based plus ownership-based authorisation, and validation on every input.
- **57 automated tests** (unit and integration) run in CI.

## Quick start

```bash
docker compose up --build          # PostgreSQL + app → http://localhost:8080
```

For development:

```bash
cd backend  && ./mvnw spring-boot:run   # API on :8080 (H2 database seeded at ./data)
cd frontend && npm install && npm run dev   # UI on :5173
cd backend  && ./mvnw test              # 57 tests
```

Demo accounts all use the password `Password@123`:

| Role | Email |
|---|---|
| Employee | `employee@itsm.local` (also `karan@`, `pooja@`, `siddharth@`) |
| Technician | `tech.apps@itsm.local`, `tech.network@`, `tech.data@`, `tech.desktop@`, `tech.identity@`, `tech.cloud@` |
| Team lead | `lead.infra@itsm.local`, `lead.apps@itsm.local` |
| Administrator | `admin@itsm.local` |
| IT manager | `manager@itsm.local` |

- API docs: <http://localhost:8080/swagger-ui.html>
- Health: `/actuator/health`

## Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `SLA_TIME_SCALE` | `10` | Divides SLA policy minutes, so breaches and escalations show up during a live demo. Set to `1` for real time. |
| `SLA_MONITOR_INTERVAL_MS` | `10000` | How often the background SLA monitor runs |
| `JWT_SECRET` | dev value | HMAC secret, at least 32 bytes. **Override in production.** |
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` | — | PostgreSQL connection (`prod` profile) |
| `EMAIL_ENABLED`, `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` | off | Real SMTP e-mail. When off, e-mail is simulated and logged. |
| `SEED_ENABLED` | `true` | Seed demo data when the database is empty |

## Deploy to Render

1. Push this repository to GitHub.
2. In Render: **New → Blueprint**, then pick the repository. `render.yaml` creates a free PostgreSQL database and the Docker web service, and generates `JWT_SECRET`.
3. Open the service URL. The first start seeds the demo data, which takes about a minute on the free tier.

## Repository layout

```
backend/   Spring Boot application (domain, strategies, events, intelligence, services, web, security)
frontend/  React single-page app (employee portal, technician console, admin dashboard)
docs/      report.md / report.pdf, UML & ER diagrams, screenshots, schema.sql, demo script
```

See [`docs/report.md`](docs/report.md) for the full project report: analysis, requirements, architecture, UML, design patterns, algorithms, database design, testing, and the technology selection report.
