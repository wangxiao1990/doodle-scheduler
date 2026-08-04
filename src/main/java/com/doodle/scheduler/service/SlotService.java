package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.repository.SlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {
    private final SlotRepository slotRepository;

    @Transactional
    public Slot createSlot(String userId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);
        checkOverlaps(userId, start, end, null);

        Slot slot = new Slot(UUID.randomUUID().toString(), userId, start, end);
        return slotRepository.save(slot);
    }

    @Transactional
    public Slot updateSlot(String slotId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.isAvailable()) {
            throw new RuntimeException("Cannot update booked slot");
        }

        // Check overlaps with other slots (excluding this one)
        checkOverlaps(slot.getUserId(), start, end, slotId);

        slot.setStartTime(start);
        slot.setEndTime(end);
        return slotRepository.save(slot);
    }

    @Transactional
    public void deleteSlot(String slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        if (!slot.isAvailable()) {
            throw new RuntimeException("Cannot delete booked slot");
        }
        slotRepository.delete(slot);
    }

    public List<Slot> getUserSlots(String userId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);

        return slotRepository.findByUserIdAndStartTimeBetween(userId, start, end);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times cannot be null");
        }
        if (start.isAfter(end) || start.equals(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    private void checkOverlaps(String userId, LocalDateTime start, LocalDateTime end, String excludeSlotId) {
        List<Slot> overlapping = slotRepository.findOverlapping(userId, start, end);

        if (excludeSlotId != null) {
            List<Slot> filteredOverlapping = overlapping.stream()
                    .filter(slot -> !slot.getId().equals(excludeSlotId))
                    .toList();
            if (!filteredOverlapping.isEmpty()) {
                throw new RuntimeException("Slot overlaps with existing slot: " +
                        overlapping.getFirst().getId());
            }
        } else {
            if (!overlapping.isEmpty()) {
                throw new RuntimeException("Slot overlaps with existing slot: " +
                        overlapping.getFirst().getId());
            }
        }
    }
}
