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
Draft → Published → Ongoing → Completed
```

---

## Project Structure by Phase

### ✅ Phase 0 — Foundation *(completed)*

Core event and ticketing system already in place.

**Files:**
- `Event.java` — Event entity model
- `Ticket.java` — Ticket entity with QR data
- `EventController.java` — CRUD endpoints for events
- `QRService.java` — QR code generation logic
- `event.html` — Event UI pages

**Features covered:**
- Event creation, update, and delete
- Student registration for events
- QR code generation per registration
- Ticket system
- Event listing and detail UI

---

### Phase 1 — User Authentication & Roles(✅comepleted)

Adds Spring Security so the app knows who is a Student, Organizer, or Admin. All subsequent phases depend on authenticated users.

**New files:**
- `User.java` — User entity with role field
- `UserController.java` — Register/login endpoints
- `SecurityConfig.java` — Spring Security configuration
- `login.html` — Login page
- `register.html` — Registration page

**Features to build:**
- Spring Security login and registration flow
- Role-based access control (Student, Organizer, Admin)
- Session management and password hashing (BCrypt)
- Role-based redirect after login
- Auth guards on EventController endpoints

**Builds on:** EventController (add `@PreAuthorize` guards)

---

### Phase 2 — QR Check-in & Attendance Tracking(✅completed)

Activates the QR codes generated in Phase 0. Organizers can scan tickets at the venue; the system validates and logs each check-in.

**New files:**
- `CheckInController.java` — Scan and validate QR endpoint
- `AttendanceRecord.java` — Per-check-in log entity
- `checkin.html` — Organizer check-in interface
- `scanner.html` — Camera-based QR scanner (JavaScript)

**Features to build:**
- QR code scanning via browser camera (JS library e.g. `html5-qrcode`)
- Server-side ticket validation (match QR data to ticket)
- Duplicate scan prevention — reject already-used tickets
- Mark ticket status as `USED` after successful check-in
- Attendance log per event (who checked in, timestamp)
- Live running count of checked-in attendees

**Builds on:** `QRService.java`, `Ticket.java` (add `status` field)

---

### Phase 3 — Live Crowd Monitoring & Alerts

The core "smart" feature. Uses attendance data from Phase 2 to track live capacity and trigger alerts when thresholds are crossed.

**New files:**
- `CrowdService.java` — Capacity tracking and threshold logic
- `AlertService.java` — Notification trigger logic
- `CrowdController.java` — REST endpoint for live count data
- `crowd-dashboard.html` — Live capacity view for organizers

**Features to build:**
- Live attendee count vs. event capacity
- Threshold alerts (configurable, e.g. 80% and 100%)
- Real-time UI updates via WebSocket or polling
- Event status badges: `Open` / `Nearly Full` / `Full`
- Email or in-app notification when capacity is reached
- Auto-close registration when event is full

**Builds on:** `AttendanceRecord.java`, `Event.java` (add `capacity` field)

---

### Phase 4 — Organizer Dashboard & Event Lifecycle

Gives organizers full visibility and control over their events, including the state machine transitions and attendee management.

**New files:**
- `OrganizerController.java` — Organizer-specific endpoints
- `organizer-dashboard.html` — Dashboard: all events, live stats
- `event-status.html` — State machine controls per event

**Features to build:**
- Event state transitions: `Draft → Published → Ongoing → Completed`
- View full registration and attendance list per event
- Download attendee list as CSV
- Close or cancel an event early
- Per-event stats: registered count, checked-in count, no-shows

**Builds on:** Phase 1 role system, Phase 3 crowd data

---

### Phase 5 — Admin Monitoring & Reports

System-wide visibility for the Admin role. Aggregates data across all events and produces downloadable reports.

**New files:**
- `AdminController.java` — Admin-only endpoints
- `ReportService.java` — Aggregate stats and export logic
- `admin-dashboard.html` — All events overview
- `reports.html` — Filter and download reports

**Features to build:**
- System-wide event overview (all organizers, all events)
- Crowd analytics across events (peak attendance, capacity trends)
- Generate and download PDF or CSV reports
- Filter reports by date range, department, or organizer
- Event state diagram view per event
- Audit log: who checked in, at what time, which scanner

**Builds on:** All prior phases — full data is available by this stage

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring MVC |
| Security | Spring Security |
| Frontend | Thymeleaf / HTML + JavaScript |
| QR Scanning | `html5-qrcode` (JS library) |
| Real-time | WebSocket or HTTP polling |
| Database | JPA / Hibernate (MySQL or H2) |
| Reports | CSV export / PDF generation |

---

## Getting Started

1. Clone the repository
2. Configure your database in `application.properties`
3. Run the Spring Boot application
4. Navigate to `/register` to create an account
5. Log in and select your role to get started

---

## Development Order

Build and demo after each phase — each phase is independently functional:

```
Phase 0 (done) → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5
   Foundation      Auth      Check-in   Crowd      Organizer   Admin
```
