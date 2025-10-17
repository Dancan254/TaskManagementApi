package com.javaguy.tms.models.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TagResponseDTO {
    private Long id;
    private String name;
    private long taskCount;
}
