# Smart Event & Crowd Management System — OOAD Design Brief

> Read this entire document before starting any diagram. All diagrams must be consistent with the entity definitions and relationships defined here.

---

## Table of Contents

- [System Overview](#system-overview)
- [Entities & Attributes](#entities--attributes)
- [Class Diagram](#class-diagram)
- [Use Case Diagram](#use-case-diagram)
- [Activity Diagram](#activity-diagram)
- [Sequence Diagram](#sequence-diagram)
- [State Diagram](#state-diagram)
- [Deployment Diagram](#deployment-diagram)
- [Consistency Checklist](#consistency-checklist)

---

## System Overview

The system has three actors: **Student**, **Organizer**, and **Admin**.

Core entities are `User`, `Event`, `Registration`, `Ticket`, and `CheckIn`. Events follow a strict lifecycle. Tickets are QR-code-based and tied 1:1 to registrations.

---

## Entities & Attributes

> These are the source of truth. Every diagram must use these exact field names and types.

### `User`
| Field | Type |
|---|---|
| userId | Long (PK) |
| name | String |
| email | String |
| passwordHash | String |
| role | Enum: `STUDENT` / `ORGANIZER` / `ADMIN` |
| createdAt | DateTime |

### `Event`
| Field | Type |
|---|---|
| eventId | Long (PK) |
| title | String |
| description | String |
| venue | String |
| startTime | DateTime |
| endTime | DateTime |
| maxCapacity | Integer |
| currentCount | Integer |
| status | Enum: `DRAFT` / `PUBLISHED` / `ONGOING` / `COMPLETED` / `CANCELLED` |
| createdBy | FK → User |

### `Registration`
| Field | Type |
|---|---|
| registrationId | Long (PK) |
| eventId | FK → Event |
| userId | FK → User |
| registeredAt | DateTime |
| status | Enum: `CONFIRMED` / `CANCELLED` |

### `Ticket`
| Field | Type |
|---|---|
| ticketId | Long (PK) |
| registrationId | FK → Registration |
| qrCodeData | String (UUID) |
| issuedAt | DateTime |
| isUsed | Boolean |

### `CheckIn`
| Field | Type |
|---|---|
| checkInId | Long (PK) |
| ticketId | FK → Ticket |
| scannedAt | DateTime |
| scannedBy | FK → User (Organizer) |

### `Alert`
| Field | Type |
|---|---|
| alertId | Long (PK) |
| eventId | FK → Event |
| message | String |
| sentAt | DateTime |
| threshold | Integer (e.g. 90 for 90%) |

---

## Class Diagram

### Relationships

| Relationship | Multiplicity | Description |
|---|---|---|
| `User` → `Event` | 1 to many | One organizer creates many events |
| `User` → `Registration` | 1 to many | One student has many registrations |
| `Event` → `Registration` | 1 to many | One event has many registrations |
| `Registration` → `Ticket` | 1 to 1 | One registration generates one ticket |
| `Ticket` → `CheckIn` | 1 to 0..1 | Check-in only exists after QR scan |
| `Event` → `Alert` | 1 to many | One event can trigger multiple alerts |

### Key Methods

**`Event`**
```
+ publish()
+ start()
+ complete()
+ cancel()
+ isFull(): boolean
```

**`Registration`**
```
+ cancel()
```

**`Ticket`**
```
+ generateQR()
+ markUsed()
```

**`CheckIn`**
```
+ validate()
```

**`AlertService`** *(service class, not an entity)*
```
+ checkCapacity(event: Event)
+ sendAlert(event: Event)
```

### Additional Notes
- Add a `<<enumeration>>` block for `EventStatus` (DRAFT, PUBLISHED, ONGOING, COMPLETED, CANCELLED)
- Add a `<<enumeration>>` block for `UserRole` (STUDENT, ORGANIZER, ADMIN)
- Mark `AlertService` with a `<<service>>` stereotype — it is not a data entity

---

## Use Case Diagram

### Actor: Student
- Register for Event
- View Registered Events
- Cancel Registration
- Download Ticket (QR)

### Actor: Organizer
- Create Event
- Edit Event
- Publish Event
- Scan QR Code (Check-In)
- View Attendance
- Start Event
- Complete Event

### Actor: Admin
- Monitor All Events (Live)
- View Crowd Capacity
- Generate Reports
- Manage Users
- Send / View Alerts

### Relationships

**`<<include>>`** (always happens as part of the base use case)
- "Register for Event" includes → "Generate Ticket"
- "Register for Event" includes → "Check Capacity"
- "Scan QR Code" includes → "Validate Ticket"

**`<<extend>>`** (happens only under a condition)
- "Check Capacity" extends → "Send Alert" *(only when capacity ≥ 90%)*

---

## Activity Diagram

**Recommended flow:** Student Registration + Check-In

**Swimlanes:** `Student` | `System` | `Organizer`

```
Student browses published events
  → Student selects event
    → [System] Check capacity
      → [Full] Show "Event Full" → END
      → [Available]
          → Student submits registration
          → [System] Create Registration record
          → [System] Generate Ticket with QR code
          → [System] Email ticket to student

On event day:
  → Student presents QR code
  → Organizer scans it
  → [System] Validate ticket
    → [Invalid / Already Used] Show error → END
    → [Valid]
        → [System] Mark ticket as used
        → [System] Increment currentCount
        → [System] Check if currentCount / maxCapacity ≥ 0.9
          → [Threshold hit] Send Alert to Admin
        → Record CheckIn → END
```

---

## Sequence Diagram

**Recommended scenario:** QR Check-In Flow

### Participants (left to right)
1. `Organizer`
2. `CheckInController`
3. `TicketService`
4. `TicketRepository`
5. `EventService`
6. `AlertService`
7. `NotificationService`

### Message Flow

```
Organizer → CheckInController       : scanTicket(qrCode)
CheckInController → TicketService   : validateTicket(qrCode)
TicketService → TicketRepository    : findByQRCode(qrCode)
TicketRepository → TicketService    : returns Ticket

[TicketService checks ticket.isUsed]
  → [true]  return error "Already Checked In"
  → [false]
      TicketService → TicketRepository   : markUsed(ticketId)
      TicketService → EventService       : incrementCount(eventId)

      [EventService checks currentCount / maxCapacity ≥ 0.9]
        → alt [capacity ≥ 90%]
            EventService → AlertService          : triggerAlert(eventId)
            AlertService → NotificationService   : sendToAdmin(alert)
        → else
            (no alert)

CheckInController → Organizer : return success response
```

> **Note:** Model the capacity check as a UML `alt` combined fragment with two branches: `[capacity ≥ 90%]` and `[else]`.

---

## State Diagram

**Entity:** `Event`

### States & Transitions

```
[Initial] ──► DRAFT ──[publish()]──► PUBLISHED ──[start()]──► ONGOING ──[complete()]──► COMPLETED
                                         │                        │
                                    [cancel()]              [cancel()]
                                         │                        │
                                         └──────────► CANCELLED ◄─┘
```

### Guard Conditions

| Transition | Guard |
|---|---|
| `publish()` | `title`, `venue`, `startTime`, `maxCapacity` must all be set |
| `start()` | `currentTime ≥ startTime` |
| `complete()` | `currentTime ≥ endTime` |

> Write guard conditions in square brackets on transition arrows: e.g. `[currentTime ≥ startTime]`

### Entry Actions

| State | Entry Action |
|---|---|
| `PUBLISHED` | Notify subscribed students |
| `ONGOING` | Begin live capacity tracking |
| `COMPLETED` | Generate attendance report |

---

## Deployment Diagram

### Nodes & Components

**Client Tier** *(Browser / Mobile)*
- React / HTML frontend
- QR Scanner component

**Web/App Server** *(Tomcat + Spring MVC)*
- `EventController`
- `RegistrationController`
- `CheckInController`
- `TicketService`
- `AlertService`
- Deployed as: `smart-event-system.war` `<<artifact>>`

**Database Server** *(MySQL)*
- Tables: `users`, `events`, `registrations`, `tickets`, `checkins`, `alerts`

**Email Server** *(SMTP — Gmail API or SendGrid)*
- Ticket delivery
- Alert notifications

**QR Code Service** *(ZXing library or external API)*
- Called by `TicketService` to generate QR images

### Connections

| From | To | Protocol |
|---|---|---|
| Browser | Tomcat | HTTPS |
| Tomcat | MySQL | JDBC |
| Tomcat | Email Server | SMTP |
| Tomcat | QR Code Service | HTTP / library call |

---

## Consistency Checklist

Before submitting any diagram, verify all of the following:

- [ ] Every class in the class diagram appears in at least one use case, sequence, or activity diagram
- [ ] `EventStatus` enum values — `DRAFT`, `PUBLISHED`, `ONGOING`, `COMPLETED`, `CANCELLED` — are identical across the class diagram and state diagram
- [ ] The alert threshold (≥ 90% capacity) appears consistently in the activity diagram, sequence diagram, and state diagram (as an ONGOING entry action)
- [ ] `Ticket.isUsed` is set in the sequence diagram and declared as an attribute in the class diagram
- [ ] `CheckIn.scannedBy` in the class diagram maps to the **Organizer** actor in the use case and sequence diagrams
- [ ] No actor, class, or transition appears in one diagram but is absent from or contradicted by another
