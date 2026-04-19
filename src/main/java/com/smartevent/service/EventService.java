package com.smartevent.service;

import com.smartevent.model.*;
import com.smartevent.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class EventService {
    private final EventRepository eventRepo;
    private final TicketRepository ticketRepo;
    private final QRService qrService;

    public EventService(EventRepository e, TicketRepository t, QRService q){
        this.eventRepo=e; this.ticketRepo=t; this.qrService=q;
    }

    public List<Event> allEvents(){ return eventRepo.findAll(); }
    public Event saveEvent(Event event){ return eventRepo.save(event); }

    public Ticket register(Long eventId, String studentName) throws Exception {
        Event event = eventRepo.findById(eventId).orElseThrow();
        if(event.getCurrentCount() >= event.getCapacity()) throw new RuntimeException("Full capacity reached");
        event.setCurrentCount(event.getCurrentCount()+1);
        eventRepo.save(event);

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setStudentName(studentName);
        ticket = ticketRepo.save(ticket);
        String qr = qrService.generateQR("TICKET:"+ticket.getId()+" EVENT:"+event.getTitle(), ticket.getId());
        ticket.setQrPath(qr);
        return ticketRepo.save(ticket);
    }
}