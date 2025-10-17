package com.javaguy.tms.util;

import com.javaguy.tms.models.dto.TaskResponseDTO;
import com.javaguy.tms.models.entity.Tag;
import com.javaguy.tms.models.entity.Task;

import java.util.Set;
import java.util.stream.Collectors;

public class TaskMapper {

    public static TaskResponseDTO toDto(Task task) {
        Set<String> tags = task.getTags().stream().map(Tag::getName).collect(Collectors.toSet());
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .status(task.getStatus())
                .calendarEventId(task.getCalendarEventId())
                .version(task.getVersion())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .tags(tags)
                .build();
    }
}
