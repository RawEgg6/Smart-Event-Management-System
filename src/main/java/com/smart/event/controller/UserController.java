package com.smart.event.controller;

import com.smart.event.entity.Role;
import com.smart.event.entity.User;
import com.smart.event.repository.TicketRepository;
import com.smart.event.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketRepository ticketRepository; // Added TicketRepository

    // Updated Constructor
    public UserController(UserRepository userRepository, 
                          PasswordEncoder passwordEncoder, 
                          TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("roles", Role.values());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest registerRequest, Model model) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            model.addAttribute("error", "Username already exists");
            model.addAttribute("roles", Role.values());
            return "register";
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            model.addAttribute("error", "Email already exists");
            model.addAttribute("roles", Role.values());
            return "register";
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(registerRequest.getRole());
        userRepository.save(user);

        return "redirect:/login?registered";
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/events";
    }

    // UPDATED STUDENT HOME METHOD
    @GetMapping("/student/home")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentHome(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Fetch tickets and add to the template
        model.addAttribute("tickets", ticketRepository.findByUserIdOrderByIdDesc(user.getId()));
        
        return "student-home";
    }

    @GetMapping("/organizer/home")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public String organizerHome() {
        return "organizer-home";
    }

    @GetMapping("/admin/home")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminHome() {
        return "admin-home";
    }

    public static class RegisterRequest {
        @NotBlank
        private String username;

        @Email
        @NotBlank
        private String email;

        @Size(min = 6)
        private String password;

        private Role role = Role.STUDENT;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
    }
}