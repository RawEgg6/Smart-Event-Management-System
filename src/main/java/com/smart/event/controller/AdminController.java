package com.smart.event.controller;

import com.smart.event.entity.Event;
import com.smart.event.entity.Role;
import com.smart.event.entity.User;
import com.smart.event.repository.EventRepository;
import com.smart.event.repository.UserRepository;
import com.smart.event.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private ReportService  reportService;
    @Autowired private EventRepository eventRepository;
    @Autowired private UserRepository  userRepository;

    // ── Admin dashboard ───────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats",     reportService.getSystemStats());
        model.addAttribute("snapshots", reportService.getEventSnapshots(null, null, null));
        model.addAttribute("organizers", userRepository.findByRole(Role.ORGANIZER));
        return "admin-dashboard";
    }

    // ── Reports page ──────────────────────────────────────────────────────────

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Model model) {

        List<Map<String, Object>> snapshots = reportService.getEventSnapshots(organizerId, from, to);
        List<User> organizers = userRepository.findByRole(Role.ORGANIZER);
        List<Event> allEvents = eventRepository.findAll();

        model.addAttribute("snapshots",   snapshots);
        model.addAttribute("organizers",  organizers);
        model.addAttribute("allEvents",   allEvents);
        model.addAttribute("organizerId", organizerId);
        model.addAttribute("from",        from);
        model.addAttribute("to",          to);
        return "reports";
    }

    // ── CSV downloads ─────────────────────────────────────────────────────────

    @GetMapping("/reports/csv")
    public void downloadReportCsv(
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletResponse response) throws IOException {

        reportService.writeEventReportCsv(response, organizerId, from, to);
    }

    @GetMapping("/reports/audit-csv")
    public void downloadAuditCsv(
            @RequestParam(required = false) Long eventId,
            HttpServletResponse response) throws IOException {

        reportService.writeAuditLogCsv(response, eventId);
    }
}