package com.smart.event.controller;

import com.smart.event.service.AlertService;
import com.smart.event.service.CrowdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/crowd")
public class CrowdController {

    @Autowired private CrowdService crowdService;
    @Autowired private AlertService alertService;

    // Polled every 5 s by the dashboard JS
    @GetMapping("/{eventId}/snapshot")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Map<String, Object>> snapshot(@PathVariable Long eventId) {
        Map<String, Object> data = crowdService.getCrowdSnapshot(eventId);

        // Attach alert info
        String alertMsg  = alertService.getAlertMessage(eventId);
        String alertLevel = alertService.getAlertLevel(eventId).name();
        data.put("alertMessage", alertMsg);
        data.put("alertLevel",   alertLevel);

        return ResponseEntity.ok(data);
    }

    // Quick registration-open check (called before allowing new sign-ups)
    @GetMapping("/{eventId}/registration-open")
    public ResponseEntity<Map<String, Object>> registrationOpen(@PathVariable Long eventId) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("open", crowdService.isRegistrationOpen(eventId));
        return ResponseEntity.ok(resp);
    }
}