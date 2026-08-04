package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Meeting;
import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.exception.InvalidTimeRangeException;
import com.doodle.scheduler.exception.SlotNotAvailableException;
import com.doodle.scheduler.repository.MeetingRepository;
import com.doodle.scheduler.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private MeetingService meetingService;

    private String userId;
    private String slotId;
    private LocalDateTime start;
    private LocalDateTime end;
    private Slot availableSlot;

    @BeforeEach
    void setUp() {
        userId = "user1";
        slotId = UUID.randomUUID().toString();
        start = LocalDateTime.now().plusHours(1);
        end = start.plusHours(1);
        availableSlot = new Slot(slotId, userId, start, end);
    }
    
    @Test
    void shouldBookMeetingSuccessfully() {
        // Given
        String title = "Team Sync";
        String description = "Weekly team meeting";

        when(slotRepository.findById(slotId))
                .thenReturn(Optional.of(availableSlot));
        when(meetingRepository.save(any(Meeting.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        Meeting result = meetingService.bookMeeting(slotId, title, description, anySet());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSlotId()).isEqualTo(slotId);
        assertThat(result.getOrganizerId()).isEqualTo(userId);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getStartTime()).isEqualTo(start);
        assertThat(result.getEndTime()).isEqualTo(end);

        // Verify slot was booked
        assertThat(availableSlot.isAvailable()).isFalse();
        verify(slotRepository).findById(slotId);
        verify(slotRepository).save(availableSlot);
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void shouldBookMeetingWithoutDescription() {
        // Given
        String title = "Quick Sync";

        when(slotRepository.findById(slotId))
                .thenReturn(Optional.of(availableSlot));
        when(meetingRepository.save(any(Meeting.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        Meeting result = meetingService.bookMeeting(slotId, title, null, anySet());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getDescription()).isNull();
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void shouldThrowWhenSlotAlreadyBooked() {
        // Given
        availableSlot.book();
        when(slotRepository.findById(slotId))
                .thenReturn(Optional.empty()); // Not found because status != AVAILABLE

        // When & Then
        assertThatThrownBy(() -> meetingService.bookMeeting(slotId, "Test", null, anySet()))
                .isInstanceOf(SlotNotAvailableException.class);

        verify(slotRepository).findById(slotId);
        verify(slotRepository, never()).save(any(Slot.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void shouldThrowWhenSlotDoesNotExist() {
        // Given
        String nonExistentSlotId = UUID.randomUUID().toString();
        when(slotRepository.findById(nonExistentSlotId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingService.bookMeeting(nonExistentSlotId, "Test", null, anySet()))
                .isInstanceOf(SlotNotAvailableException.class);

        verify(slotRepository).findById(nonExistentSlotId);
        verify(slotRepository, never()).save(any(Slot.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void shouldThrowWhenBookingWithEmptyTitle() {
        // When & Then
        assertThatThrownBy(() -> meetingService.bookMeeting(slotId, "", null, Collections.emptySet()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title cannot be empty");

        assertThatThrownBy(() -> meetingService.bookMeeting(slotId, null, null, Collections.emptySet()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title cannot be empty");

        // Verify no save was attempted
        verify(slotRepository, never()).save(any(Slot.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }
    
    @Test
    void shouldGetUserMeetingsSuccessfully() {
        // Given
        Meeting meeting1 = new Meeting(
                UUID.randomUUID().toString(),
                slotId,
                userId,
                "Meeting 1",
                start,
                end
        );
        Meeting meeting2 = new Meeting(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                userId,
                "Meeting 2",
                start.plusHours(2),
                end.plusHours(2)
        );
        List<Meeting> expectedMeetings = List.of(meeting1, meeting2);

        when(meetingRepository.findByOrganizerIdAndStartTimeBetween(userId, start, end))
                .thenReturn(expectedMeetings);

        // When
        List<Meeting> result = meetingService.getUserMeetings(userId, start, end);

        // Then
        assertThat(result).hasSize(2).containsExactly(meeting1, meeting2);
        verify(meetingRepository).findByOrganizerIdAndStartTimeBetween(userId, start, end);
    }

    @Test
    void shouldReturnEmptyListWhenNoMeetings() {
        // Given
        when(meetingRepository.findByOrganizerIdAndStartTimeBetween(userId, start, end))
                .thenReturn(List.of());

        // When
        List<Meeting> result = meetingService.getUserMeetings(userId, start, end);

        // Then
        assertThat(result).isEmpty();
        verify(meetingRepository).findByOrganizerIdAndStartTimeBetween(userId, start, end);
    }

    @Test
    void shouldThrowWhenGettingMeetingsWithInvalidTimes() {
        assertThatThrownBy(() -> meetingService.getUserMeetings(userId, null, end))
                .isInstanceOf(InvalidTimeRangeException.class);
        assertThatThrownBy(() -> meetingService.getUserMeetings(userId, start, null))
                .isInstanceOf(InvalidTimeRangeException.class);
        LocalDateTime invalidEnd = start.minusHours(1);
        assertThatThrownBy(() -> meetingService.getUserMeetings(userId, start, invalidEnd))
                .isInstanceOf(InvalidTimeRangeException.class);
    }
}