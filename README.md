# Smart Event & Crowd Management System

A web-based college event management platform built with **Spring MVC**. Handles event creation, student registrations, QR-based ticketing, live crowd monitoring, and admin reporting.

---

## Overview

| Role | Capabilities |
|---|---|
| **Student / User** | Browse events, register, receive QR ticket, attend |
| **Organizer** | Create and manage events, view attendees, track check-ins |
| **Admin** | Monitor all events system-wide, generate reports |

### Event Lifecycle (State Machine)
```
Draft → Published → Ongoing → Completed → (or Cancelled)
```

---

## Project Package Structure

```
com.smart.event
├── controller/
│   ├── EventController.java
│   ├── UserController.java
│   ├── CheckInController.java
│   ├── CrowdController.java        ← Phase 3
│   └── OrganizerController.java    ← Phase 4
├── entity/
│   ├── Event.java
│   ├── User.java
│   ├── Ticket.java
│   └── AttendanceRecord.java
├── repository/
│   ├── EventRepository.java
│   ├── UserRepository.java
│   ├── TicketRepository.java
│   └── AttendanceRecordRepository.java
└── service/
    ├── QRService.java
    ├── CrowdService.java            ← Phase 3
    └── AlertService.java            ← Phase 3
```

**Template location:** `src/main/resources/templates/`

---

## Entity Field Reference

> ⚠️ Critical for LLM context — always use these exact field names in Java and Thymeleaf templates.

### `Event.java`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key |
| `title` | `String` | Use `getTitle()` — NOT `getName()` |
| `description` | `String` | |
| `location` | `String` | |
| `startTime` | `LocalDateTime` | Use `getStartTime()` — NOT `getDate()` |
| `endTime` | `LocalDateTime` | |
| `capacity` | `int` | Added in Phase 3 |
| `status` | `EventStatus` (enum) | Added in Phase 3/4 |
| `organizer` | `User` | FK to users table as `organizer_id` |

**EventStatus enum values:** `DRAFT`, `PUBLISHED`, `ONGOING`, `COMPLETED`, `CANCELLED`

### `User.java`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key |
| `username` | `String` | Use `getUsername()` — NOT `getName()` |
| `email` | `String` | |
| `password` | `String` | BCrypt encoded |
| `role` | `Role` (enum) | `STUDENT`, `ORGANIZER`, `ADMIN` |

### `Ticket.java`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key |
| `qrData` | `String` | Unique QR code string |
| `status` | `TicketStatus` (enum) | `UNUSED`, `USED` |
| `generatedAt` | `LocalDateTime` | Use `getGeneratedAt()` — NOT `getCreatedAt()` |
| `event` | `Event` | FK |
| `user` | `User` | FK |

### `AttendanceRecord.java`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key |
| `checkInTime` | `LocalDateTime` | |
| `scannedBy` | `String` | Username of organizer who scanned |
| `user` | `User` | FK — student who checked in |
| `event` | `Event` | FK |
| `ticket` | `Ticket` | FK |

---

## Repository Method Reference

### `EventRepository`
```java
List<Event> findByOrganizerId(Long organizerId);   // used in OrganizerController
Optional<Event> findById(Long id);
List<Event> findAll();                              // used in AdminController (Phase 5)
```

### `TicketRepository`
```java
List<Ticket> findByEventIdOrderByIdDesc(Long eventId);
int countByEventId(Long eventId);
Optional<Ticket> findByQrData(String qrData);
```

### `AttendanceRecordRepository`
```java
List<AttendanceRecord> findByEventId(Long eventId);
int countByEventId(Long eventId);
```

### `UserRepository`
```java
Optional<User> findByUsername(String username);
Optional<User> findByEmail(String email);
List<User> findByRole(Role role);                  // useful for Phase 5 admin
```

---

## Phase 0 — Foundation ✅

Core event and ticketing system.

**Files:** `Event.java`, `Ticket.java`, `EventController.java`, `QRService.java`, `event.html`

**Features:** Event CRUD, student registration, QR code generation, ticket system, event listing UI.

---

## Phase 1 — User Authentication & Roles ✅

**Files:** `User.java`, `UserController.java`, `SecurityConfig.java`, `login.html`, `register.html`

**Features:** Spring Security login/registration, BCrypt password hashing, role-based access (`STUDENT`, `ORGANIZER`, `ADMIN`), session management, `@PreAuthorize` guards on controllers.

**Security config key points:**
- `/register`, `/login`, `/h2-console/**` are permitted without auth
- H2 console requires `csrf.ignoringRequestMatchers("/h2-console/**")` and `headers.frameOptions.disable()`
- `@AuthenticationPrincipal` injects `UserDetails`, not the `User` entity directly — always look up the `User` entity via `userRepository.findByUsername(userDetails.getUsername())`

---

## Phase 2 — QR Check-in & Attendance Tracking ✅

**Files:** `CheckInController.java`, `AttendanceRecord.java`, `checkin.html`, `scanner.html`

**Features:** Browser camera QR scanning (`html5-qrcode` JS library), server-side ticket validation, duplicate scan prevention, mark ticket as `USED`, attendance log with timestamp, live check-in count.

---

## Phase 3 — Live Crowd Monitoring & Alerts ✅

### New Files Added

**`CrowdService.java`**
- `getLiveCount(Long eventId)` → calls `attendanceRecordRepository.countByEventId()`
- `getRegisteredCount(Long eventId)` → calls `ticketRepository.countByEventId()`
- `getCrowdStatus(Long eventId)` → returns `CrowdStatus` enum: `OPEN`, `NEARLY_FULL`, `FULL`
  - `NEARLY_FULL` threshold: ≥ 80% capacity
  - `FULL` threshold: ≥ 100% capacity
- `getCrowdSnapshot(Long eventId)` → returns `Map<String, Object>` with keys: `eventId`, `eventName`, `checkedIn`, `registered`, `capacity`, `status`, `percent`
- `isRegistrationOpen(Long eventId)` → returns false when status is `FULL`

**`AlertService.java`**
- `getAlertLevel(Long eventId)` → returns `AlertLevel` enum: `NONE`, `WARNING`, `CRITICAL`
- `getAlertMessage(Long eventId)` → returns human-readable alert string or null
- `isNewAlert(Long eventId)` → tracks state changes using in-memory `ConcurrentHashMap`

**`CrowdController.java`** (`/api/crowd/`)
- `GET /api/crowd/{eventId}/snapshot` → returns full JSON snapshot including alert info; requires `ORGANIZER` or `ADMIN` role
- `GET /api/crowd/{eventId}/registration-open` → returns `{"open": true/false}`

**`crowd-dashboard.html`**
- Polls `/api/crowd/{eventId}/snapshot` every **5 seconds** via `fetch()`
- Shows: checked-in count, registered count, capacity, % full
- Capacity bar changes color: green (OPEN) → yellow (NEARLY_FULL) → red (FULL)
- Alert banner appears at top when WARNING or CRITICAL
- `eventId` is injected via Thymeleaf: `const eventId = /*[[${eventId}]]*/ 1;`

### Snapshot JSON Shape
```json
{
  "eventId": 1,
  "eventName": "Tech Fest",
  "checkedIn": 4,
  "registered": 5,
  "capacity": 5,
  "status": "NEARLY_FULL",
  "percent": 80,
  "alertMessage": "⚠️ Event \"Tech Fest\" is nearly full (80%+ capacity).",
  "alertLevel": "WARNING"
}
```

---

## Phase 4 — Organizer Dashboard & Event Lifecycle ✅

### New Files Added

**`OrganizerController.java`** (`/organizer/`, requires `ORGANIZER` role)

Key pattern — `@AuthenticationPrincipal` gives `UserDetails`, not `User`:
```java
// Always resolve User entity like this:
User organizer = userRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
```

Endpoints:
- `GET /organizer/dashboard` → lists all events for logged-in organizer using `findByOrganizerId()`; attaches crowd snapshot for each
- `GET /organizer/events/{id}` → event detail: tickets, attendance records, no-show count, snapshot
- `POST /organizer/events/{id}/transition?targetStatus=PUBLISHED` → state machine transition with guard logic
- `GET /organizer/events/{id}/attendees/csv` → CSV download via OpenCSV
- `GET /organizer/crowd/{eventId}` → renders `crowd-dashboard.html`

**State machine transition guards:**
```
DRAFT      → PUBLISHED or (nothing else)
PUBLISHED  → ONGOING or CANCELLED
ONGOING    → COMPLETED or CANCELLED
COMPLETED  → (terminal, no transitions)
CANCELLED  → (terminal, no transitions)
```

**`organizer-dashboard.html`**
- Table columns: Event, Date, Status, Registered, Checked In, Capacity, Crowd, Actions
- Actions per row: Details button → `/organizer/events/{id}`, Live button → `/organizer/crowd/{id}`, CSV button → `/organizer/events/{id}/attendees/csv`
- Uses `snapshots[iter.index]` to display per-event crowd stats alongside Thymeleaf event loop

**`event-status.html`**
- Left card: Event lifecycle state machine (visual nodes: DRAFT → PUBLISHED → ONGOING → COMPLETED), transition buttons shown conditionally based on current status
- Right card: Live stats (registered, checked-in, no-shows, capacity) + CSV download button
- Bottom card: Registrations & Attendance table

### CSV Export
Uses **OpenCSV 5.9** (`com.opencsv:opencsv:5.9` in `pom.xml`).
CSV headers: `Student Name, Email, Ticket ID, Status, Registered At`
Uses `ticket.user.username` and `ticket.generatedAt` (not `name` or `createdAt`).

---

## Phase 5 — Admin Monitoring & Reports (✅completed)

**Files to create:**
- `AdminController.java` — Admin-only endpoints (`/admin/`)
- `ReportService.java` — Aggregate stats and export logic
- `admin-dashboard.html` — All events overview across all organizers
- `reports.html` — Filter and download reports

**Features to build:**
- System-wide event overview (all organizers, all events) using `eventRepository.findAll()`
- Crowd analytics across events (peak attendance, capacity trends)
- Generate and download PDF or CSV reports
- Filter reports by date range or organizer
- Audit log view: who checked in, at what time, which scanner (`AttendanceRecord.scannedBy`)

**Key data available for Phase 5:**
- All events: `eventRepository.findAll()`
- All check-ins: `attendanceRecordRepository.findAll()` or `findByEventId()`
- All tickets: `ticketRepository.findAll()` or `findByEventId()`
- All users by role: `userRepository.findByRole(Role.ORGANIZER)`
- Live crowd snapshot for any event: `crowdService.getCrowdSnapshot(eventId)`

**Security:** All `/admin/**` endpoints must be guarded with `@PreAuthorize("hasRole('ADMIN')")`.

**PDF generation suggestion:** Use `iText` or `Apache PDFBox` library. Add to `pom.xml` before starting.

**Builds on:** All prior phases — all data is available. Inject and reuse `CrowdService` for live stats per event.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3.4, Spring MVC |
| Security | Spring Security 6.3.3 |
| Frontend | Thymeleaf 3.1, HTML + JavaScript |
| QR Scanning | `html5-qrcode` (JS library) |
| Real-time | HTTP Polling (5s interval fetch) |
| Database | JPA / Hibernate 6.5, H2 (in-memory, resets on restart) |
| CSV Export | OpenCSV 5.9 |
| Build | Maven (Spring Boot 3.3.4) |

**H2 Console:** `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:eventdb`
- Username: `SA`, Password: *(blank)*

> ⚠️ H2 is in-memory — all data is lost on every restart and must be re-seeded.

---

## Re-seed SQL (run after every restart)

```sql
-- Assumes organizer has id=1 and student has id=2
-- Register accounts via /register first, then check ids with:
-- SELECT id, username, role FROM users;

INSERT INTO events (title, description, location, start_time, end_time, capacity, status, organizer_id)
VALUES ('Tech Fest', 'Annual tech event', 'Main Hall', '2026-05-01 10:00:00', '2026-05-01 18:00:00', 5, 'DRAFT', 1);

INSERT INTO tickets (qr_data, status, generated_at, event_id, user_id)
VALUES ('QR-TEST-001', 'UNUSED', NOW(), 1, 2);

INSERT INTO attendance_records (check_in_time, scanned_by, user_id, event_id, ticket_id)
VALUES (NOW(), 'organizer1', 2, 1, 1);
```

---

## Development Order

```
Phase 0 (✅) → Phase 1 (✅) → Phase 2 (✅) → Phase 3 (✅) → Phase 4 (✅) → Phase 5 🔜
  Foundation      Auth         Check-in       Crowd          Organizer       Admin
```
