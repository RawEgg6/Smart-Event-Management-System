package com.smart.event.service;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QRService {

    public String generateQrData(Long eventId, Long userId) {
        return "EVT-" + eventId + "-USR-" + userId + "-" + UUID.randomUUID();
    }
}
