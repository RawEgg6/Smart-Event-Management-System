package com.smart.event.service;

import com.smart.event.entity.Event;
import com.smart.event.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertService {

    @Autowired
    private CrowdService crowdService;

    @Autowired
    private EventRepository eventRepository;

    public enum AlertLevel { NONE, WARNING, CRITICAL }

    // Tracks last alert level per event so we don't spam the same banner
    private final Map<Long, AlertLevel> lastAlertState = new ConcurrentHashMap<>();

    public AlertLevel getAlertLevel(Long eventId) {
        CrowdService.CrowdStatus status = crowdService.getCrowdStatus(eventId);
        return switch (status) {
            case FULL        -> AlertLevel.CRITICAL;
            case NEARLY_FULL -> AlertLevel.WARNING;
            default          -> AlertLevel.NONE;
        };
    }

    // Returns alert message string for in-app display
    public String getAlertMessage(Long eventId) {
        AlertLevel level = getAlertLevel(eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return switch (level) {
            case CRITICAL -> "🚨 Event \"" + event.getTitle() + "\" has reached full capacity!";
            case WARNING  -> "⚠️ Event \"" + event.getTitle() + "\" is nearly full (80%+ capacity).";   
            default       -> null;
        };
    }

    // Returns whether the alert is new (state changed) — avoids repeated banners
    public boolean isNewAlert(Long eventId) {
        AlertLevel current  = getAlertLevel(eventId);
        AlertLevel previous = lastAlertState.getOrDefault(eventId, AlertLevel.NONE);
        if (!current.equals(previous)) {
            lastAlertState.put(eventId, current);
            return current != AlertLevel.NONE;
        }
        return false;
    }
}