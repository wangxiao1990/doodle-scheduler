package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Meeting;
import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.exception.InvalidTimeRangeException;
import com.doodle.scheduler.exception.SlotNotAvailableException;
import com.doodle.scheduler.repository.MeetingRepository;
import com.doodle.scheduler.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final SlotRepository slotRepository;

    @Transactional
    public Meeting bookMeeting(String slotId, String title, String description, Set<String> participants) {
        validateTitle(title);

        Slot slot = slotRepository.findAvailableSlotForUpdate(slotId)
                .orElseThrow(() -> {
                    log.warn("Slot not available for booking: {}", slotId);
                    return new SlotNotAvailableException(slotId);
                });

        try {
            slot.book();
            slotRepository.save(slot);
        } catch (IllegalStateException e) {
            log.error("Failed to book slot {}: {}", slotId, e.getMessage());
            throw new SlotNotAvailableException(slotId);
        }

        Meeting meeting = new Meeting(
                UUID.randomUUID().toString(),
                slotId,
                slot.getUserId(),
                title,
                slot.getStartTime(),
                slot.getEndTime()
        );
        meeting.setDescription(description);
        meeting.setParticipants(participants);
        return meetingRepository.save(meeting);
    }

    public List<Meeting> getUserMeetings(String userId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);

        return meetingRepository.findByOrganizerIdAndStartTimeBetween(userId, start, end);
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new InvalidTimeRangeException("Start and end times cannot be null");
        }
        if (start.isAfter(end) || start.equals(end)) {
            throw new InvalidTimeRangeException(start, end);
        }
    }
}
