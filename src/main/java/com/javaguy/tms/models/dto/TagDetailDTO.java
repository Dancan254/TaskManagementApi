package com.javaguy.tms.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDetailDTO {
    private Long id;
    private String name;
    private Long taskCount;
    private List<TaskResponseDTO> tasks;
}
