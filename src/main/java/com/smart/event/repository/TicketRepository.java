package com.smart.event.repository;

import com.smart.event.entity.Ticket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByQrData(String qrData);

    List<Ticket> findByEventIdOrderByIdDesc(Long eventId);
    int countByEventId(Long eventId);
}
