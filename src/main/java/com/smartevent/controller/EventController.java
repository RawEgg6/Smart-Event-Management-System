package com.smartevent.controller;

import com.smartevent.model.*;
import com.smartevent.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/events")
public class EventController {
    private final EventService service;
    public EventController(EventService service){ this.service = service; }

    @GetMapping
    public String events(Model model){
        model.addAttribute("events", service.allEvents());
        return "events";
    }

    @GetMapping("/create")
    public String createPage(Model model){
        model.addAttribute("event", new Event());
        return "create-event";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Event event){
        service.saveEvent(event);
        return "redirect:/events";
    }

    @PostMapping("/register/{id}")
    public String register(@PathVariable Long id, @RequestParam String studentName, Model model) throws Exception {
        Ticket ticket = service.register(id, studentName);
        model.addAttribute("ticket", ticket);
        return "ticket";
    }
}