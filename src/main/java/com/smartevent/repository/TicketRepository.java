package com.smartevent.repository;
import com.smartevent.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TicketRepository extends JpaRepository<Ticket, Long> {}