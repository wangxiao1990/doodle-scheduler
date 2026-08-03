package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Meeting;
import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.repository.MeetingRepository;
import com.doodle.scheduler.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final SlotRepository slotRepository;

    @Transactional
    public Meeting bookMeeting(String slotId, String title, String description, Set<String> participants) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not available"));

        slot.book();
        slotRepository.save(slot);

        Meeting meeting = new Meeting(
                UUID.randomUUID().toString(),
                slotId,
                slot.getUserId(),
                title,
                slot.getStartTime(),
                slot.getEndTime()
        );
        meeting.setDescription(description);
        return meetingRepository.save(meeting);
    }

    public List<Meeting> getUserMeetings(String userId, LocalDateTime start, LocalDateTime end) {
        return meetingRepository.findByOrganizerIdAndStartTimeBetween(userId, start, end);
    }
}
