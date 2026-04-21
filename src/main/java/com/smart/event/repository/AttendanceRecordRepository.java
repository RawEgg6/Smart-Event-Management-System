package com.smart.event.repository;

import com.smart.event.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    long countByEventId(Long eventId);

    boolean existsByTicketId(Long ticketId);

    List<AttendanceRecord> findAllByOrderByCheckInTimeDesc();
}