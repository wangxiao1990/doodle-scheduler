package com.doodle.scheduler.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InvalidTimeRangeException extends RuntimeException {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public InvalidTimeRangeException(LocalDateTime startTime, LocalDateTime endTime) {
        super("Invalid time range: start " + startTime + " must be before end " + endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public InvalidTimeRangeException(String message) {
        super(message);
        this.startTime = null;
        this.endTime = null;
    }

}
