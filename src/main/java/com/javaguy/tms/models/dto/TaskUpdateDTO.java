package com.javaguy.tms.models.dto;

import com.javaguy.tms.models.enums.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class TaskUpdateDTO {
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;
    @Size(max = 2048, message = "Description must be less than 2048 characters")
    private String description;
    private LocalDateTime dueDate;
    private TaskStatus status;
    private Long version;
    private Set<String> tags;
}
