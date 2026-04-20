package com.smart.event.controller;

import com.smart.event.entity.AttendanceRecord;
import com.smart.event.entity.Ticket;
import com.smart.event.entity.TicketStatus;
import com.smart.event.repository.AttendanceRecordRepository;
import com.smart.event.repository.TicketRepository;
import java.util.ArrayList;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CheckInController {

    private final TicketRepository ticketRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public CheckInController(TicketRepository ticketRepository,
                             AttendanceRecordRepository attendanceRecordRepository) {
        this.ticketRepository = ticketRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @GetMapping("/checkin")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public String checkinPage() {
        return "checkin";
    }

    @GetMapping("/scanner")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public String scannerPage() {
        return "scanner";
    }

    @PostMapping("/checkin/scan")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public Map<String, Object> scanTicket(@RequestBody ScanRequest scanRequest, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        Ticket ticket = ticketRepository.findByQrData(scanRequest.getQrData()).orElse(null);
        if (ticket == null) {
            response.put("success", false);
            response.put("message", "Invalid QR ticket");
            return response;
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            response.put("success", false);
            response.put("message", "Ticket already used");
            response.put("eventId", ticket.getEvent().getId());
            return response;
        }

        ticket.setStatus(TicketStatus.USED);
        ticketRepository.save(ticket);

        AttendanceRecord record = new AttendanceRecord();
        record.setEvent(ticket.getEvent());
        record.setAttendee(ticket.getUser());
        record.setTicket(ticket);
        record.setCheckInTime(LocalDateTime.now());
        record.setScannedBy(principal.getName());
        attendanceRecordRepository.save(record);

        long checkedInCount = attendanceRecordRepository.countByEventId(ticket.getEvent().getId());

        response.put("success", true);
        response.put("message", "Check-in successful");
        response.put("eventId", ticket.getEvent().getId());
        response.put("eventTitle", ticket.getEvent().getTitle());
        response.put("attendee", ticket.getUser().getUsername());
        response.put("checkedInCount", checkedInCount);
        return response;
    }

    @GetMapping("/checkin/event/{eventId}/count")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public Map<String, Object> liveCount(@PathVariable Long eventId) {
        Map<String, Object> response = new HashMap<>();
        response.put("eventId", eventId);
        response.put("checkedInCount", attendanceRecordRepository.countByEventId(eventId));
        return response;
    }

    @GetMapping("/checkin/event/{eventId}/records")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public String attendanceRecords(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("count", attendanceRecordRepository.countByEventId(eventId));
        return "checkin";
    }

    @GetMapping("/checkin/event/{eventId}/tickets")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public List<Map<String, Object>> eventTickets(@PathVariable Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventIdOrderByIdDesc(eventId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Ticket ticket : tickets) {
            Map<String, Object> ticketData = new HashMap<>();
            ticketData.put("ticketId", ticket.getId());
            ticketData.put("eventId", ticket.getEvent().getId());
            ticketData.put("username", ticket.getUser().getUsername());
            ticketData.put("qrData", ticket.getQrData());
            ticketData.put("status", ticket.getStatus().name());
            response.add(ticketData);
        }

        return response;
    }

    public static class ScanRequest {
        private String qrData;

        public String getQrData() {
            return qrData;
        }

        public void setQrData(String qrData) {
            this.qrData = qrData;
        }
    }
}
