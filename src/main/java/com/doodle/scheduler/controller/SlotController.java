package com.doodle.scheduler.controller;

import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/slots")
@RequiredArgsConstructor
public class SlotController {
    private final SlotService slotService;

    @PostMapping
    public ResponseEntity<Slot> createSlot(@RequestBody CreateSlotRequest request) {
        Slot slot = slotService.createSlot(
                request.userId(), request.startTime(), request.endTime()
        );
        return ResponseEntity.ok(slot);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Slot> updateSlot(@PathVariable String id, @RequestBody UpdateSlotRequest request) {
        Slot slot = slotService.updateSlot(id, request.startTime(), request.endTime());
        return ResponseEntity.ok(slot);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable String id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Slot>> getUserSlots(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(slotService.getUserSlots(userId, start, end));
    }
}

record CreateSlotRequest(String userId, LocalDateTime startTime, LocalDateTime endTime) {}
record UpdateSlotRequest(LocalDateTime startTime, LocalDateTime endTime) {}
