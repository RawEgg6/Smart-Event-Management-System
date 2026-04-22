package com.smart.event.repository;

import com.smart.event.entity.Event;
import com.smart.event.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizerId(Long organizerId);
}