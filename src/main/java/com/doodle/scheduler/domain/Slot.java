package com.doodle.scheduler.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Slot {
    @Id
    private String id;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private SlotStatus status = SlotStatus.AVAILABLE;

    public Slot(String id, String userId, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isAvailable() {
        return status == SlotStatus.AVAILABLE;
    }

    public void book() {
        if (!isAvailable()) {
            throw new IllegalStateException("Slot not available");
        }
        this.status = SlotStatus.BOOKED;
    }
}

enum SlotStatus {
    AVAILABLE, BOOKED
}
