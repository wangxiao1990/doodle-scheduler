package com.doodle.scheduler.controller;

import com.doodle.scheduler.domain.Meeting;
import com.doodle.scheduler.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<Meeting> bookMeeting(@RequestBody BookMeetingRequest request) {
        Meeting meeting = meetingService.bookMeeting(
                request.slotId(), request.title(), request.description(), request.participants()
        );
        return ResponseEntity.ok(meeting);
    }

    @GetMapping
    public ResponseEntity<List<Meeting>> getUserMeetings(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(meetingService.getUserMeetings(userId, start, end));
    }
}

record BookMeetingRequest(String slotId, String title, String description, Set<String> participants) {}
