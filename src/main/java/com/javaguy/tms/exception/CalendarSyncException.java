package com.javaguy.tms.exception;

public class CalendarSyncException extends RuntimeException {
    public CalendarSyncException(String message) {
        super(message);
    }

    public CalendarSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
