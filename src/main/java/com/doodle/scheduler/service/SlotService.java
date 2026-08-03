package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.repository.SlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotService {
    private final SlotRepository slotRepository;

    @Transactional
    public Slot createSlot(String userId, LocalDateTime start, LocalDateTime end, String title) {
        // Check for overlaps
        List<Slot> overlapping = slotRepository.findOverlapping(userId, start, end);
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Slot overlaps with existing slot");
        }

        Slot slot = new Slot(UUID.randomUUID().toString(), userId, start, end);
        return slotRepository.save(slot);
    }

    @Transactional
    public Slot updateSlot(String slotId, LocalDateTime start, LocalDateTime end) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.isAvailable()) {
            throw new RuntimeException("Cannot update booked slot");
        }

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
        return slotRepository.findByUserIdAndStartTimeBetween(userId, start, end);
    }
}
