package com.smart.event.controller;

import com.smart.event.entity.AttendanceRecord;
import com.smart.event.entity.Event;
import com.smart.event.repository.AttendanceRecordRepository;
import com.smart.event.repository.EventRepository;
import com.smart.event.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private ReportService reportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Event> allEvents   = eventRepository.findAll();
        long totalEvents        = allEvents.size();
        long totalCheckins      = attendanceRecordRepository.count();
        Map<String, Long> eventsByStatus  = reportService.countEventsByStatus();
        Map<String, Long> checkinsByEvent = reportService.checkinCountPerEvent();

        model.addAttribute("allEvents",       allEvents);
        model.addAttribute("totalEvents",     totalEvents);
        model.addAttribute("totalCheckins",   totalCheckins);
        model.addAttribute("eventsByStatus",  eventsByStatus);
        model.addAttribute("checkinsByEvent", checkinsByEvent);
        return "admin/admin-dashboard";
    }

    @GetMapping("/reports")
    public String reportsPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String organizer,
            Model model) {

        List<Event> filtered    = reportService.filterEvents(from, to, organizer);
        List<String> organizers = reportService.getAllOrganizerNames();

        model.addAttribute("events",     filtered);
        model.addAttribute("organizers", organizers);
        model.addAttribute("from",       from);
        model.addAttribute("to",         to);
        model.addAttribute("organizer",  organizer);
        return "admin/reports";
    }

    @GetMapping("/audit")
    public String auditLog(Model model) {
        List<AttendanceRecord> logs = attendanceRecordRepository.findAllByOrderByCheckInTimeDesc();
        model.addAttribute("logs", logs);
        return "admin/audit-log";
    }

    @GetMapping("/reports/download/csv")
    public void downloadCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String organizer,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"event-report.csv\"");
        reportService.exportCsv(from, to, organizer, response.getWriter());
    }

    @GetMapping("/reports/download/pdf")
    public void downloadPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String organizer,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"event-report.pdf\"");
        reportService.exportPdf(from, to, organizer, response.getOutputStream());
    }
}