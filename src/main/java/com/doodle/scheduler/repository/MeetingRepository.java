package com.doodle.scheduler.repository;

import com.doodle.scheduler.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
}