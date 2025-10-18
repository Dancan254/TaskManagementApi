package com.javaguy.tms.models.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateDTO {
    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;
    @Size(max = 2048, message = "Description must be less than 2048 characters")
    private String description;
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;
    private Set<String> tags;
}
