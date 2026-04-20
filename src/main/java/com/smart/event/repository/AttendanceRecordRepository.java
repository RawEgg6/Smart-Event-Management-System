package com.smart.event.repository;

import com.smart.event.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    long countByEventId(Long eventId);
}
