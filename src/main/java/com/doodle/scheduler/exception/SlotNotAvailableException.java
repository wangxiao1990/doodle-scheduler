package com.doodle.scheduler.exception;

import lombok.Getter;

@Getter
public class SlotNotAvailableException extends RuntimeException {
    private final String slotId;
    private final String reason;

    public SlotNotAvailableException(String slotId) {
        super("Slot is not available: " + slotId);
        this.slotId = slotId;
        this.reason = "Slot is booked";
    }

    public SlotNotAvailableException(String slotId, String reason) {
        super("Slot is not available: " + slotId + ". Reason: " + reason);
        this.slotId = slotId;
        this.reason = reason;
    }

}
