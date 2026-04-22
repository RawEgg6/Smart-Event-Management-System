package com.smart.event.controller;
import com.smart.event.repository.UserRepository;
import com.smart.event.entity.AttendanceRecord;
import com.smart.event.entity.Event;
import com.smart.event.entity.Ticket;
import com.smart.event.entity.User;
import com.smart.event.repository.AttendanceRecordRepository;
import com.smart.event.repository.EventRepository;
import com.smart.event.repository.TicketRepository;
import com.smart.event.service.CrowdService;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/organizer")
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerController {

    @Autowired private EventRepository            eventRepository;
    @Autowired private TicketRepository           ticketRepository;
    @Autowired private AttendanceRecordRepository attendanceRecordRepository;
    @Autowired private CrowdService               crowdService;
    @Autowired private UserRepository userRepository;

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails, Model model) {
        // Look up the actual User entity by username
        User organizer = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Event> events = eventRepository.findByOrganizerId(organizer.getId());

        List<Map<String, Object>> snapshots = events.stream()
                .map(e -> crowdService.getCrowdSnapshot(e.getId()))
                .toList();

        model.addAttribute("events",    events);
        model.addAttribute("snapshots", snapshots);
        return "organizer-dashboard";
    }

    // ── Event detail (registrations + attendance) ──────────────────────────────

    @GetMapping("/events/{id}")
    public String eventDetail(@PathVariable Long id, Model model) {
        Event event = findEvent(id);
        List<Ticket> tickets = ticketRepository.findByEventIdOrderByIdDesc(id);
        List<AttendanceRecord> attendance = attendanceRecordRepository.findByEventId(id);

        int noShows = tickets.size() - attendance.size();

        model.addAttribute("event",      event);
        model.addAttribute("tickets",    tickets);
        model.addAttribute("attendance", attendance);
        model.addAttribute("noShows",    Math.max(noShows, 0));
        model.addAttribute("snapshot",   crowdService.getCrowdSnapshot(id));
        return "event-status";
    }

    // ── State machine transitions ─────────────────────────────────────────────

    @PostMapping("/events/{id}/transition")
    public String transition(@PathVariable Long id,
                             @RequestParam Event.EventStatus targetStatus) {
        Event event = findEvent(id);

        // Guard valid forward transitions
        boolean valid = switch (event.getStatus()) {
            case DRAFT      -> targetStatus == Event.EventStatus.PUBLISHED;
            case PUBLISHED  -> targetStatus == Event.EventStatus.ONGOING
                            || targetStatus == Event.EventStatus.CANCELLED;
            case ONGOING    -> targetStatus == Event.EventStatus.COMPLETED
                            || targetStatus == Event.EventStatus.CANCELLED;
            default         -> false;   // COMPLETED / CANCELLED are terminal
        };

        if (valid) {
            event.setStatus(targetStatus);
            eventRepository.save(event);
        }
        return "redirect:/organizer/events/" + id;
    }

    // ── CSV download ──────────────────────────────────────────────────────────

    @GetMapping("/events/{id}/attendees/csv")
    public void downloadCsv(@PathVariable Long id,
                            HttpServletResponse response) throws IOException {
        Event event = findEvent(id);
        List<Ticket> tickets = ticketRepository.findByEventIdOrderByIdDesc(id);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"attendees-" + id + ".csv\"");

        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {

            // Header row
            writer.writeNext(new String[]{"Student Name", "Email", "Ticket ID",
                                          "Status", "Registered At"});
            // Data rows
            for (Ticket t : tickets) {
                writer.writeNext(new String[]{
                    t.getUser().getUsername(),
                    t.getUser().getEmail(),
                    String.valueOf(t.getId()),
                    t.getStatus().name(),
                    t.getGeneratedAt() != null ? t.getGeneratedAt().toString() : ""
                });
            }
        }
    }

    // ── Live crowd dashboard (delegates to crowd-dashboard.html) ──────────────

    @GetMapping("/crowd/{eventId}")
    public String crowdDashboard(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "crowd-dashboard";
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
    }
}