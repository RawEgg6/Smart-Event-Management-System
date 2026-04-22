package com.smart.event.controller;

import com.smart.event.entity.Event;
import com.smart.event.entity.Event.EventStatus;
import com.smart.event.entity.Ticket;
import com.smart.event.entity.TicketStatus;
import com.smart.event.entity.User;
import com.smart.event.repository.EventRepository;
import com.smart.event.repository.TicketRepository;
import com.smart.event.repository.UserRepository;
import com.smart.event.service.QRService;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

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
        // Hides DRAFT and CANCELLED events from the public browsing page
        List<Event> activeEvents = eventRepository.findAll().stream()
            .filter(e -> e.getStatus() == EventStatus.PUBLISHED || e.getStatus() == EventStatus.ONGOING)
            .collect(Collectors.toList());
            
        model.addAttribute("events", activeEvents);
        return "events";
    }


    @PostMapping("/events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public String createEvent(@RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String location,
                              @RequestParam int capacity,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                              Principal principal) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setCapacity(capacity);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setStatus(Event.EventStatus.DRAFT); 

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

        // Guard 1: Event must be published to accept registrations
        if (event.getStatus() != EventStatus.PUBLISHED) {
            return "redirect:/events?error=NotPublished";
        }

        // Guard 2: Prevent multiple tickets per student
        if (ticketRepository.existsByEventIdAndUserId(event.getId(), user.getId())) {
            return "redirect:/events?error=AlreadyRegistered";
        }

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setUser(user);
        ticket.setQrData(qrService.generateQrData(event.getId(), user.getId()));
        ticket.setStatus(TicketStatus.UNUSED);
        ticketRepository.save(ticket);

        return "redirect:/student/home";
    }
}