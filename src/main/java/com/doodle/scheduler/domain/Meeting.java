package com.doodle.scheduler.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
public class Meeting {
    @Id
    private String id;
    private String slotId;
    private String organizerId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ElementCollection
    private Set<String> participants = new HashSet<>();

    public Meeting(String id, String slotId, String organizerId, String title,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.slotId = slotId;
        this.organizerId = organizerId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}