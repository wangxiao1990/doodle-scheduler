package com.doodle.scheduler.repository;

import com.doodle.scheduler.domain.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotRepository extends JpaRepository<Slot, String> {
}
