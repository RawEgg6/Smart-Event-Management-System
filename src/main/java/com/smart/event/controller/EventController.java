package com.smart.event.controller;

import com.smart.event.entity.Event;
import com.smart.event.entity.Ticket;
import com.smart.event.entity.TicketStatus;
import com.smart.event.entity.User;
import com.smart.event.repository.EventRepository;
import com.smart.event.repository.TicketRepository;
import com.smart.event.repository.UserRepository;
import com.smart.event.service.QRService;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EventController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final QRService qrService;

    public EventController(EventRepository eventRepository,
                           UserRepository userRepository,
                           TicketRepository ticketRepository,
                           QRService qrService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.qrService = qrService;
    }

    @GetMapping("/events")
    public String listEvents(Model model) {
        model.addAttribute("events", eventRepository.findAll());
        return "events";
    }

    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public String createEvent(@RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String location,
                              Principal principal) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);

        if (principal != null) {
            User organizer = userRepository.findByUsername(principal.getName()).orElse(null);
            event.setOrganizer(organizer);
        }

        eventRepository.save(event);
        return "redirect:/events";
    }

    @PostMapping("/events/{eventId}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public String registerForEvent(@PathVariable Long eventId, Principal principal) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setUser(user);
        ticket.setQrData(qrService.generateQrData(event.getId(), user.getId()));
        ticket.setStatus(TicketStatus.UNUSED);
        ticketRepository.save(ticket);

        return "redirect:/events";
    }
}
