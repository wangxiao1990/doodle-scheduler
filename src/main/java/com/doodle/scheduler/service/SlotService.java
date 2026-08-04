package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.exception.InvalidTimeRangeException;
import com.doodle.scheduler.exception.SlotNotAvailableException;
import com.doodle.scheduler.exception.SlotNotFoundException;
import com.doodle.scheduler.exception.SlotOverlapException;
import com.doodle.scheduler.repository.SlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotService {
    private final SlotRepository slotRepository;

    @Transactional
    public Slot createSlot(String userId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);

        try {
            checkOverlaps(userId, start, end, null);
        } catch (SlotOverlapException e) {
            log.warn("Overlap detected when creating slot for user {}: {}", userId, e.getMessage());
            throw e;
        }

        Slot slot = new Slot(UUID.randomUUID().toString(), userId, start, end);
        return slotRepository.save(slot);
    }

    @Transactional
    public Slot updateSlot(String slotId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> {
                    log.warn("Slot not found: {}", slotId);
                    return new SlotNotFoundException(slotId);
                });

        if (!slot.isAvailable()) {
            log.warn("Attempted to update booked slot: {}", slotId);
            throw new SlotNotAvailableException(slotId, "Cannot update a slot that is already booked");
        }

        // Check overlaps with other slots (excluding this one)
        try {
            checkOverlaps(slot.getUserId(), start, end, slotId);
        } catch (SlotOverlapException e) {
            log.warn("Overlap detected when updating slot {}: {}", slotId, e.getMessage());
            throw e;
        }

        slot.setStartTime(start);
        slot.setEndTime(end);
        return slotRepository.save(slot);
    }

    @Transactional
    public void deleteSlot(String slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> {
                    log.warn("Slot not found: {}", slotId);
                    return new SlotNotFoundException(slotId);
                });

        if (!slot.isAvailable()) {
            log.warn("Attempted to delete booked slot: {}", slotId);
            throw new SlotNotAvailableException(slotId, "Cannot delete a slot that is already booked");
        }

        slotRepository.delete(slot);
    }

    public List<Slot> getUserSlots(String userId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);

        return slotRepository.findByUserIdAndStartTimeBetween(userId, start, end);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new InvalidTimeRangeException("Start and end times cannot be null");
        }
        if (start.isAfter(end) || start.equals(end)) {
            throw new InvalidTimeRangeException(start, end);
        }
    }

    private void checkOverlaps(String userId, LocalDateTime start, LocalDateTime end, String excludeSlotId) {
        List<Slot> overlapping = slotRepository.findOverlapping(userId, start, end);

        if (excludeSlotId != null) {
            List<Slot> filteredOverlapping = overlapping.stream()
                    .filter(slot -> !slot.getId().equals(excludeSlotId))
                    .toList();
            if (!filteredOverlapping.isEmpty()) {
                throw new SlotOverlapException("Slot overlaps with existing slot: " +
                        overlapping.getFirst().getId());
            }
        } else {
            if (!overlapping.isEmpty()) {
                throw new SlotOverlapException("Slot overlaps with existing slot: " +
                        overlapping.getFirst().getId());
            }
        }
    }
}
