package com.javaguy.tms.models.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ErrorResponse {
    private String error;
    private String message;
    private int status;
    private String path;
    private OffsetDateTime timestamp;
}
