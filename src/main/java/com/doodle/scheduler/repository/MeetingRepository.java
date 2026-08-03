package com.doodle.scheduler.repository;

import com.doodle.scheduler.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    List<Meeting> findByOrganizerIdAndStartTimeBetween(String organizerId, LocalDateTime start, LocalDateTime end);
}