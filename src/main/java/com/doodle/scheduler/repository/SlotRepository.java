package com.doodle.scheduler.repository;

import com.doodle.scheduler.domain.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, String> {
    List<Slot> findByUserIdAndStartTimeBetween(String userId, LocalDateTime start, LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id AND s.status = 'AVAILABLE'")
    Optional<Slot> findAvailableSlotForUpdate(@Param("id") String id);

    @Query("""
    SELECT s FROM Slot s WHERE s.userId = :userId AND ((s.startTime < :end AND s.endTime > :start))
    """)
    List<Slot> findOverlapping(@Param("userId") String userId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);
}
