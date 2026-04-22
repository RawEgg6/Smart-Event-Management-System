package com.smart.event.service;

import com.smart.event.entity.Event;
import com.smart.event.entity.AttendanceRecord;
import com.smart.event.repository.AttendanceRecordRepository;
import com.smart.event.repository.EventRepository;
import com.smart.event.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CrowdService {

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    // Returns live checked-in count for an event
    public int getLiveCount(Long eventId) {
        return attendanceRecordRepository.countByEventId(eventId);
    }

    // Returns registered (ticket-holding) count for an event
    public int getRegisteredCount(Long eventId) {
        return ticketRepository.countByEventId(eventId);
    }

    public enum CrowdStatus {
        OPEN, NEARLY_FULL, FULL
    }

    // Determine crowd status based on thresholds
    public CrowdStatus getCrowdStatus(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        int checkedIn = getLiveCount(eventId);
        int capacity   = event.getCapacity();
        double ratio   = (double) checkedIn / capacity;

        if (ratio >= 1.0)  return CrowdStatus.FULL;
        if (ratio >= 0.8)  return CrowdStatus.NEARLY_FULL;
        return CrowdStatus.OPEN;
    }

    // Full snapshot for the REST endpoint
    public Map<String, Object> getCrowdSnapshot(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        int checkedIn  = getLiveCount(eventId);
        int registered = getRegisteredCount(eventId);
        int capacity   = event.getCapacity();
        CrowdStatus status = getCrowdStatus(eventId);

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("eventId",    eventId);
        snapshot.put("eventName", event.getTitle());
        snapshot.put("checkedIn",  checkedIn);
        snapshot.put("registered", registered);
        snapshot.put("capacity",   capacity);
        snapshot.put("status",     status.name());           // "OPEN" / "NEARLY_FULL" / "FULL"
        snapshot.put("percent",    capacity > 0 ? (checkedIn * 100 / capacity) : 0);
        return snapshot;
    }

    // Should registration be blocked?
    public boolean isRegistrationOpen(Long eventId) {
        return getCrowdStatus(eventId) != CrowdStatus.FULL;
    }
}