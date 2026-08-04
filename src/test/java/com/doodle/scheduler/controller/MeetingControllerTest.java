package com.doodle.scheduler.controller;

import com.doodle.scheduler.domain.Meeting;
import com.doodle.scheduler.exception.InvalidTimeRangeException;
import com.doodle.scheduler.exception.SlotNotAvailableException;
import com.doodle.scheduler.exception.SlotNotFoundException;
import com.doodle.scheduler.service.MeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MeetingService meetingService;

    private String userId;
    private String slotId;
    private String meetingId;
    private LocalDateTime start;
    private LocalDateTime end;
    private String title;
    private String description;
    private Set<String> participants;
    private Meeting testMeeting;

    @BeforeEach
    void setUp() {
        userId = "user1";
        slotId = UUID.randomUUID().toString();
        meetingId = UUID.randomUUID().toString();
        start = LocalDateTime.now().plusHours(1);
        end = start.plusHours(1);
        title = "Team Sync";
        description = "Weekly sync meeting";
        participants = Set.of("user1", "user2", "user3");
        testMeeting = new Meeting(meetingId, slotId, userId, title, start, end);
        testMeeting.setDescription(description);
        testMeeting.setParticipants(participants);
    }

    // BOOK MEETING TESTS

    @Test
    void shouldBookMeetingSuccessfully() throws Exception {
        // Given
        BookMeetingRequest request = new BookMeetingRequest(slotId, title, description, participants);
        when(meetingService.bookMeeting(eq(slotId), eq(title), eq(description), eq(participants)))
                .thenReturn(testMeeting);

        // When & Then
        mockMvc.perform(post("/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(meetingId))
                .andExpect(jsonPath("$.slotId").value(slotId))
                .andExpect(jsonPath("$.organizerId").value(userId))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.participants").isArray())
                .andExpect(jsonPath("$.participants.length()").value(3))
                .andExpect(jsonPath("$.startTime").isNotEmpty())
                .andExpect(jsonPath("$.endTime").isNotEmpty());

        verify(meetingService).bookMeeting(eq(slotId), eq(title), eq(description), eq(participants));
    }

    @Test
    void shouldReturn400WhenBookMeetingWithEmptyTitle() throws Exception {
        // Given
        BookMeetingRequest request = new BookMeetingRequest(slotId, "", description, participants);
        when(meetingService.bookMeeting(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Title cannot be empty"));

        // When & Then
        mockMvc.perform(post("/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Invalid Argument"))
                .andExpect(jsonPath("$.message").value("Title cannot be empty"));

        verify(meetingService).bookMeeting(eq(slotId), eq(""), eq(description), eq(participants));
    }

    @Test
    void shouldReturn409WhenBookUnavailableSlot() throws Exception {
        // Given
        BookMeetingRequest request = new BookMeetingRequest(slotId, title, null, participants);
        when(meetingService.bookMeeting(any(), any(), any(), any()))
                .thenThrow(new SlotNotAvailableException(slotId));

        // When & Then
        mockMvc.perform(post("/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.details.slotId").value(slotId));

        verify(meetingService).bookMeeting(eq(slotId), eq(title), isNull(), eq(participants));
    }

    @Test
    void shouldReturn404WhenBookNonExistentSlot() throws Exception {
        // Given
        String nonExistentSlotId = UUID.randomUUID().toString();
        BookMeetingRequest request = new BookMeetingRequest(nonExistentSlotId, title, null, participants);
        when(meetingService.bookMeeting(any(), any(), any(),any()))
                .thenThrow(new SlotNotFoundException(nonExistentSlotId));

        // When & Then
        mockMvc.perform(post("/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));
    }

    // GET MEETINGS TESTS

    @Test
    void shouldGetUserMeetingsSuccessfully() throws Exception {
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
        List<Meeting> meetings = List.of(meeting1, meeting2);

        when(meetingService.getUserMeetings(eq(userId), eq(start), eq(end)))
                .thenReturn(meetings);

        // When & Then
        mockMvc.perform(get("/v1/meetings")
                        .param("userId", userId)
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].organizerId").value(userId))
                .andExpect(jsonPath("$[0].title").value("Meeting 1"))
                .andExpect(jsonPath("$[1].title").value("Meeting 2"));

        verify(meetingService).getUserMeetings(eq(userId), eq(start), eq(end));
    }

    @Test
    void shouldReturnEmptyListWhenNoMeetingsFound() throws Exception {
        // Given
        when(meetingService.getUserMeetings(eq(userId), eq(start), eq(end)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/v1/meetings")
                        .param("userId", userId)
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(meetingService).getUserMeetings(eq(userId), eq(start), eq(end));
    }

    @Test
    void shouldReturn400WhenGetMeetingsWithInvalidTimeRange() throws Exception {
        // Given
        LocalDateTime invalidEnd = start.minusHours(1);
        when(meetingService.getUserMeetings(any(), any(), any()))
                .thenThrow(new InvalidTimeRangeException(start, invalidEnd));

        // When & Then
        mockMvc.perform(get("/v1/meetings")
                        .param("userId", userId)
                        .param("start", start.toString())
                        .param("end", invalidEnd.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
    }
}
