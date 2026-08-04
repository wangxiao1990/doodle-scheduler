package com.doodle.scheduler.controller;

import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.exception.InvalidTimeRangeException;
import com.doodle.scheduler.exception.SlotNotAvailableException;
import com.doodle.scheduler.exception.SlotNotFoundException;
import com.doodle.scheduler.exception.SlotOverlapException;
import com.doodle.scheduler.service.SlotService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SlotController.class)
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SlotService slotService;

    private String userId;
    private String slotId;
    private LocalDateTime start;
    private LocalDateTime end;
    private Slot testSlot;

    @BeforeEach
    void setUp() {
        userId = "user1";
        slotId = UUID.randomUUID().toString();
        start = LocalDateTime.now().plusHours(1);
        end = start.plusHours(1);
        testSlot = new Slot(slotId, userId, start, end);
    }

    // CREATE SLOT TESTS

    @Test
    void shouldCreateSlotSuccessfully() throws Exception {
        // Given
        CreateSlotRequest request = new CreateSlotRequest(userId, start, end);
        when(slotService.createSlot(eq(userId), eq(start), eq(end)))
                .thenReturn(testSlot);

        // When & Then
        mockMvc.perform(post("/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slotId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(slotService).createSlot(eq(userId), eq(start), eq(end));
    }

    @Test
    void shouldReturn400WhenCreateSlotWithInvalidTimeRange() throws Exception {
        // Given
        LocalDateTime invalidEnd = start.minusHours(1);
        CreateSlotRequest request = new CreateSlotRequest(userId, start, invalidEnd);
        when(slotService.createSlot(any(), any(), any()))
                .thenThrow(new InvalidTimeRangeException(start, invalidEnd));

        // When & Then
        mockMvc.perform(post("/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
    }

    @Test
    void shouldReturn409WhenCreateSlotWithOverlap() throws Exception {
        // Given
        CreateSlotRequest request = new CreateSlotRequest(userId, start, end);
        when(slotService.createSlot(any(), any(), any()))
                .thenThrow(new SlotOverlapException("123"));

        // When & Then
        mockMvc.perform(post("/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_OVERLAP"))
                .andExpect(jsonPath("$.details.overlappingSlotId").value("123"));
    }

    // UPDATE SLOT TESTS

    @Test
    void shouldUpdateSlotSuccessfully() throws Exception {
        // Given
        LocalDateTime newStart = start.plusHours(2);
        LocalDateTime newEnd = newStart.plusHours(1);
        UpdateSlotRequest request = new UpdateSlotRequest(newStart, newEnd);

        Slot updatedSlot = new Slot(slotId, userId, newStart, newEnd);

        when(slotService.updateSlot(eq(slotId), eq(newStart), eq(newEnd)))
                .thenReturn(updatedSlot);

        // When & Then
        mockMvc.perform(put("/v1/slots/{id}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slotId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.startTime").isNotEmpty());

        verify(slotService).updateSlot(eq(slotId), eq(newStart), eq(newEnd));
    }

    @Test
    void shouldReturn404WhenUpdateNonExistentSlot() throws Exception {
        // Given
        UpdateSlotRequest request = new UpdateSlotRequest(start, end);
        when(slotService.updateSlot(eq(slotId), any(), any()))
                .thenThrow(new SlotNotFoundException(slotId));

        // When & Then
        mockMvc.perform(put("/v1/slots/{id}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Slot not found with id: " + slotId));
    }

    @Test
    void shouldReturn400WhenUpdateBookedSlot() throws Exception {
        // Given
        UpdateSlotRequest request = new UpdateSlotRequest(start, end);
        when(slotService.updateSlot(eq(slotId), any(), any()))
                .thenThrow(new SlotNotAvailableException(slotId, "Cannot update a slot that is already booked"));

        // When & Then
        mockMvc.perform(put("/v1/slots/{id}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.details.slotId").value(slotId));
    }

    @Test
    void shouldReturn409WhenUpdateSlotWithOverlap() throws Exception {
        // Given
        UpdateSlotRequest request = new UpdateSlotRequest(start, end);
        when(slotService.updateSlot(eq(slotId), any(), any()))
                .thenThrow(new SlotOverlapException("456"));

        // When & Then
        mockMvc.perform(put("/v1/slots/{id}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_OVERLAP"))
                .andExpect(jsonPath("$.details.overlappingSlotId").value("456"));
    }

    // DELETE SLOT TESTS

    @Test
    void shouldDeleteSlotSuccessfully() throws Exception {
        // Given
        doNothing().when(slotService).deleteSlot(slotId);

        // When & Then
        mockMvc.perform(delete("/v1/slots/{id}", slotId))
                .andExpect(status().isNoContent());

        verify(slotService).deleteSlot(slotId);
    }

    @Test
    void shouldReturn404WhenDeleteNonExistentSlot() throws Exception {
        // Given
        doThrow(new SlotNotFoundException(slotId)).when(slotService).deleteSlot(slotId);

        // When & Then
        mockMvc.perform(delete("/v1/slots/{id}", slotId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Slot not found with id: " + slotId));
    }

    @Test
    void shouldReturn400WhenDeleteBookedSlot() throws Exception {
        // Given
        doThrow(new SlotNotAvailableException(slotId, "Cannot delete a slot that is already booked")).when(slotService).deleteSlot(slotId);

        // When & Then
        mockMvc.perform(delete("/v1/slots/{id}", slotId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.details.slotId").value(slotId));
    }

    // GET SLOTS TESTS

    @Test
    void shouldGetUserSlotsSuccessfully() throws Exception {
        // Given
        List<Slot> slots = List.of(testSlot);
        when(slotService.getUserSlots(eq(userId), eq(start), eq(end)))
                .thenReturn(slots);

        // When & Then
        mockMvc.perform(get("/v1/slots")
                        .param("userId", userId)
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(slotId))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        verify(slotService).getUserSlots(eq(userId), eq(start), eq(end));
    }

    @Test
    void shouldReturnEmptyListWhenNoSlotsFound() throws Exception {
        // Given
        when(slotService.getUserSlots(eq(userId), eq(start), eq(end)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/v1/slots")
                        .param("userId", userId)
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(slotService).getUserSlots(eq(userId), eq(start), eq(end));
    }

    @Test
    void shouldReturn400WhenGetSlotsWithInvalidTimeRange() throws Exception {
        // Given
        LocalDateTime invalidEnd = start.minusHours(1);
        when(slotService.getUserSlots(any(), any(), any()))
                .thenThrow(new InvalidTimeRangeException(start, invalidEnd));

        // When & Then
        mockMvc.perform(get("/v1/slots")
                        .param("userId", userId)
                        .param("start", start.toString())
                        .param("end", invalidEnd.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
    }
}
