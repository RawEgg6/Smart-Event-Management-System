package com.smart.event.service;

import com.smart.event.entity.AttendanceRecord;
import com.smart.event.entity.Event;
import com.smart.event.entity.User;
import com.smart.event.repository.AttendanceRecordRepository;
import com.smart.event.repository.EventRepository;
import com.smart.event.repository.TicketRepository;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired private EventRepository            eventRepository;
    @Autowired private TicketRepository           ticketRepository;
    @Autowired private AttendanceRecordRepository attendanceRecordRepository;
    @Autowired private CrowdService               crowdService;

    // ── System-wide stats snapshot ────────────────────────────────────────────

    public Map<String, Object> getSystemStats() {
        List<Event> allEvents = eventRepository.findAll();
        int totalEvents      = allEvents.size();
        int totalRegistered  = allEvents.stream().mapToInt(e -> ticketRepository.countByEventId(e.getId())).sum();
        int totalCheckedIn   = allEvents.stream().mapToInt(e -> attendanceRecordRepository.countByEventId(e.getId())).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents",     totalEvents);
        stats.put("totalRegistered", totalRegistered);
        stats.put("totalCheckedIn",  totalCheckedIn);
        stats.put("totalNoShows",    Math.max(totalRegistered - totalCheckedIn, 0));
        return stats;
    }

    // ── All events with crowd snapshots (optionally filtered) ─────────────────

    public List<Map<String, Object>> getEventSnapshots(Long organizerId,
                                                        LocalDateTime from,
                                                        LocalDateTime to) {
        List<Event> events = eventRepository.findAll();

        // Filter by organizer
        if (organizerId != null) {
            events = events.stream()
                    .filter(e -> e.getOrganizer() != null
                              && e.getOrganizer().getId().equals(organizerId))
                    .collect(Collectors.toList());
        }

        // Filter by date range (uses startTime)
        if (from != null) {
            events = events.stream()
                    .filter(e -> e.getStartTime() != null && !e.getStartTime().isBefore(from))
                    .collect(Collectors.toList());
        }
        if (to != null) {
            events = events.stream()
                    .filter(e -> e.getStartTime() != null && !e.getStartTime().isAfter(to))
                    .collect(Collectors.toList());
        }

        return events.stream()
                .map(e -> {
                    Map<String, Object> snap = crowdService.getCrowdSnapshot(e.getId());
                    snap.put("status",        e.getStatus().name());
                    snap.put("organizerName", e.getOrganizer() != null
                                              ? e.getOrganizer().getUsername() : "N/A");
                    snap.put("startTime",     e.getStartTime());
                    return snap;
                })
                .collect(Collectors.toList());
    }

    // ── Audit log (all check-ins, optionally filtered by event) ───────────────

    public List<AttendanceRecord> getAuditLog(Long eventId) {
        if (eventId != null) {
            return attendanceRecordRepository.findByEventId(eventId);
        }
        return attendanceRecordRepository.findAll();
    }

    // ── CSV export ────────────────────────────────────────────────────────────

    public void writeEventReportCsv(jakarta.servlet.http.HttpServletResponse response,
                                     Long organizerId,
                                     LocalDateTime from,
                                     LocalDateTime to) throws IOException {

        List<Map<String, Object>> snapshots = getEventSnapshots(organizerId, from, to);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"event-report.csv\"");

        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {

            writer.writeNext(new String[]{
                "Event", "Organizer", "Start Time", "Status",
                "Capacity", "Registered", "Checked In", "No Shows", "% Full"
            });

            for (Map<String, Object> s : snapshots) {
                int registered = (int) s.get("registered");
                int checkedIn  = (int) s.get("checkedIn");
                writer.writeNext(new String[]{
                    String.valueOf(s.get("eventName")),
                    String.valueOf(s.get("organizerName")),
                    String.valueOf(s.get("startTime")),
                    String.valueOf(s.get("status")),
                    String.valueOf(s.get("capacity")),
                    String.valueOf(registered),
                    String.valueOf(checkedIn),
                    String.valueOf(Math.max(registered - checkedIn, 0)),
                    s.get("percent") + "%"
                });
            }
        }
    }

    public void writeAuditLogCsv(jakarta.servlet.http.HttpServletResponse response,
                                  Long eventId) throws IOException {

        List<AttendanceRecord> records = getAuditLog(eventId);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit-log.csv\"");

        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {

            writer.writeNext(new String[]{"Student", "Email", "Event", "Check-In Time", "Scanned By"});

            for (AttendanceRecord r : records) {
                writer.writeNext(new String[]{
                    r.getAttendee().getUsername(),
                    r.getAttendee().getEmail(),
                    r.getEvent().getTitle(),
                    r.getCheckInTime().toString(),
                    r.getScannedBy()
                });
            }
        }
    }
}