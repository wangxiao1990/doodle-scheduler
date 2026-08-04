package com.doodle.scheduler.exception;

import lombok.Getter;

@Getter
public class SlotOverlapException extends RuntimeException {
    private final String overlappingSlotId;

    public SlotOverlapException(String overlappingSlotId) {
        super("Slot overlaps with existing slot: " + overlappingSlotId);
        this.overlappingSlotId = overlappingSlotId;
    }

    public SlotOverlapException(String message, String overlappingSlotId) {
        super(message);
        this.overlappingSlotId = overlappingSlotId;
    }

}
