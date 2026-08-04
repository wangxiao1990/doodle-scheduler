package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {
    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private SlotService slotService;

    private String userId;
    private String slotId;
    private LocalDateTime start;
    private LocalDateTime end;
    private Slot existingSlot;

    @BeforeEach
    void setUp() {
        userId = "user1";
        slotId = UUID.randomUUID().toString();
        start = LocalDateTime.now().plusHours(1);
        end = start.plusHours(1);
        existingSlot = new Slot(slotId, userId, start, end);
    }

    @Test
    void shouldCreateSlotSuccessfully() {
        // Given
        when(slotRepository.findOverlapping(userId, start, end)).thenReturn(List.of());
        when(slotRepository.save(any(Slot.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Slot result = slotService.createSlot(userId, start, end);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStartTime()).isEqualTo(start);
        assertThat(result.getEndTime()).isEqualTo(end);
        assertThat(result.isAvailable()).isTrue();

        verify(slotRepository).findOverlapping(userId, start, end);
        verify(slotRepository).save(any(Slot.class));
    }

    @Test
    void shouldThrowWhenCreatingSlotWithInvalidTimes() {
        assertThatThrownBy(() -> slotService.createSlot(userId, null, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> slotService.createSlot(userId, start, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        LocalDateTime invalidEnd = start.minusHours(1);

        assertThatThrownBy(() -> slotService.createSlot(userId, start, invalidEnd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");

        assertThatThrownBy(() -> slotService.createSlot(userId, start, start))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    void shouldThrowWhenCreatingSlotWithOverlap() {
        // Given
        Slot overlappingSlot = new Slot(UUID.randomUUID().toString(), userId, start, end);
        when(slotRepository.findOverlapping(userId, start, end))
                .thenReturn(List.of(overlappingSlot));

        // When & Then
        assertThatThrownBy(() -> slotService.createSlot(userId, start, end))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("overlaps with existing slot");

        verify(slotRepository).findOverlapping(userId, start, end);
        verify(slotRepository, never()).save(any(Slot.class));
    }

    @Test
    void shouldUpdateSlotSuccessfully() {
        // Given
        LocalDateTime newStart = start.plusHours(2);
        LocalDateTime newEnd = newStart.plusHours(1);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(existingSlot));
        when(slotRepository.findOverlapping(userId, newStart, newEnd)).thenReturn(List.of());
        when(slotRepository.save(any(Slot.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Slot result = slotService.updateSlot(slotId, newStart, newEnd);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(newStart);
        assertThat(result.getEndTime()).isEqualTo(newEnd);
        assertThat(result.isAvailable()).isTrue();

        verify(slotRepository).findById(slotId);
        verify(slotRepository).findOverlapping(userId, newStart, newEnd);
        verify(slotRepository).save(existingSlot);
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentSlot() {
        // Given
        when(slotRepository.findById(slotId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> slotService.updateSlot(slotId, start, end))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Slot not found");

        verify(slotRepository).findById(slotId);
        verify(slotRepository, never()).save(any(Slot.class));
    }

    @Test
    void shouldThrowWhenUpdatingBookedSlot() {
        // Given
        existingSlot.book();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(existingSlot));

        // When & Then
        assertThatThrownBy(() -> slotService.updateSlot(slotId, start, end))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot update booked slot");

        verify(slotRepository).findById(slotId);
        verify(slotRepository, never()).findOverlapping(any(), any(), any());
        verify(slotRepository, never()).save(any(Slot.class));
    }

    @Test
    void shouldThrowWhenUpdatingSlotWithOverlap() {
        // Given
        LocalDateTime newStart = start.plusHours(2);
        LocalDateTime newEnd = newStart.plusHours(1);
        Slot overlappingSlot = new Slot(UUID.randomUUID().toString(), userId, newStart, newEnd);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(existingSlot));
        when(slotRepository.findOverlapping(userId, newStart, newEnd))
                .thenReturn(List.of(overlappingSlot));

        // When & Then
        assertThatThrownBy(() -> slotService.updateSlot(slotId, newStart, newEnd))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("overlaps with existing slot");

        verify(slotRepository).findById(slotId);
        verify(slotRepository).findOverlapping(userId, newStart, newEnd);
        verify(slotRepository, never()).save(any(Slot.class));
    }

    @Test
    void shouldAllowUpdateWhenOverlapIsWithSelf() {
        // Given
        LocalDateTime newStart = start.plusHours(2);
        LocalDateTime newEnd = newStart.plusHours(1);

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(existingSlot));
        when(slotRepository.findOverlapping(userId, newStart, newEnd))
                .thenReturn(List.of(existingSlot)); // Only overlaps with itself
        when(slotRepository.save(any(Slot.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Slot result = slotService.updateSlot(slotId, newStart, newEnd);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(newStart);

        verify(slotRepository).findOverlapping(userId, newStart, newEnd);
        verify(slotRepository).save(existingSlot);
    }

    @Test
    void shouldThrowWhenUpdatingSlotWithInvalidTimes() {
        // When & Then
        assertThatThrownBy(() -> slotService.updateSlot(slotId, null, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> slotService.updateSlot(slotId, start, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        LocalDateTime invalidEnd = start.minusHours(1);
        assertThatThrownBy(() -> slotService.updateSlot(slotId, start, invalidEnd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    void shouldDeleteSlotSuccessfully() {
        // Given
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(existingSlot));

        // When
        slotService.deleteSlot(slotId);

        // Then
        verify(slotRepository).findById(slotId);
        verify(slotRepository).delete(existingSlot);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentSlot() {
        // Given
        when(slotRepository.findById(slotId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> slotService.deleteSlot(slotId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Slot not found");

        verify(slotRepository).findById(slotId);
        verify(slotRepository, never()).delete(any(Slot.class));
    }

    @Test
    void shouldThrowWhenDeletingBookedSlot() {
        // Given
        existingSlot.book();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(existingSlot));

        // When & Then
        assertThatThrownBy(() -> slotService.deleteSlot(slotId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete booked slot");

        verify(slotRepository).findById(slotId);
        verify(slotRepository, never()).delete(any(Slot.class));
    }

    @Test
    void shouldGetUserSlotsSuccessfully() {
        // Given
        List<Slot> expectedSlots = List.of(existingSlot);
        when(slotRepository.findByUserIdAndStartTimeBetween(userId, start, end))
                .thenReturn(expectedSlots);

        // When
        List<Slot> result = slotService.getUserSlots(userId, start, end);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(existingSlot);
        verify(slotRepository).findByUserIdAndStartTimeBetween(userId, start, end);
    }

    @Test
    void shouldThrowWhenGettingSlotsWithInvalidTimes() {
        assertThatThrownBy(() -> slotService.getUserSlots(userId, null, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> slotService.getUserSlots(userId, start, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        LocalDateTime invalidEnd = start.minusHours(1);
        assertThatThrownBy(() -> slotService.getUserSlots(userId, start, invalidEnd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");
    }
}
