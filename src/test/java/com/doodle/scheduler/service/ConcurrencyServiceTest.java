package com.doodle.scheduler.service;

import com.doodle.scheduler.domain.Slot;
import com.doodle.scheduler.repository.MeetingRepository;
import com.doodle.scheduler.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrencyServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private MeetingService meetingService;

    private String slotId;
    private Slot availableSlot;

    @BeforeEach
    void setUp() {
        String userId = "user1";
        slotId = UUID.randomUUID().toString();
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(1);
        availableSlot = new Slot(slotId, userId, start, end);
    }

    @Test
    void shouldHandleConcurrentBookingWithMockedLock() throws InterruptedException {
        // Simulate pessimistic locking behavior
        when(slotRepository.findAvailableSlotForUpdate(slotId))
                .thenReturn(Optional.of(availableSlot))
                .thenReturn(Optional.empty());

        // First booking succeeds
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int concurrentThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    meetingService.bookMeeting(slotId, "Meeting", "Description", Set.of("A", "B"));
                    successCount.incrementAndGet();
                } catch (RuntimeException | InterruptedException e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Only one should succeed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(concurrentThreads - 1);

        // Verify slot was booked
        assertThat(availableSlot.isAvailable()).isFalse();
        verify(slotRepository, atLeastOnce()).findAvailableSlotForUpdate(slotId);
    }

    @Test
    void shouldBookSlotOnce() throws InterruptedException {
        // Simulate pessimistic locking
        when(slotRepository.findAvailableSlotForUpdate(slotId))
                .thenAnswer(invocation -> {
                    if (availableSlot.isAvailable()) {
                        availableSlot.book();
                        return Optional.of(availableSlot);
                    }
                    return Optional.empty();
                });

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger booked = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    Optional<Slot> slot = slotRepository.findAvailableSlotForUpdate(slotId);
                    if (slot.isPresent()) {
                        booked.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(booked.get()).isEqualTo(1);
        assertThat(availableSlot.isAvailable()).isFalse();
    }
}
