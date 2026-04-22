package com.smart.event.repository;

import com.smart.event.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    int countByEventId(Long eventId);
    List<AttendanceRecord> findByEventId(Long eventId);
}