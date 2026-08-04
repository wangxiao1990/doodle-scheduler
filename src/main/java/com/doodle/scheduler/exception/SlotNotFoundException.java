package com.doodle.scheduler.exception;

public class SlotNotFoundException extends RuntimeException {
    public SlotNotFoundException(String slotId) {
        super("Slot not found with id: " + slotId);
    }
}
