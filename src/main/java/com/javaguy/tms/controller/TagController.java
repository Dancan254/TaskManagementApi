package com.javaguy.tms.controller;

import com.javaguy.tms.models.dto.TagResponseDTO;
import com.javaguy.tms.models.dto.TaskResponseDTO;
import com.javaguy.tms.repository.TaskRepository;
import com.javaguy.tms.service.TagService;
import com.javaguy.tms.util.TaskMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tag Management", description = "APIs for managing tags")
public class TagController {

    private final TagService tagService;
    private final TaskRepository taskRepository;

    public TagController(TagService tagService, TaskRepository taskRepository) {
        this.tagService = tagService;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    @Operation(
            summary = "List all tags",
            description = "Retrieves a list of all tags along with the number of tasks associated with each tag."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of tags")
    public ResponseEntity<List<TagResponseDTO>> list() {
        List<com.javaguy.tms.models.entity.Tag> tags = tagService.getAll();
        return ResponseEntity.ok(tags.stream()
                .map(t -> TagResponseDTO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .taskCount(taskRepository.findTasksByTagName(t.getName()).size())
                        .build())
                .collect(Collectors.toList()));
    }

    @GetMapping("/{tagName}/tasks")
    @Operation(
            summary = "Get tasks for a specific tag",
            description = "Retrieves all tasks associated with a given tag name."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved tasks for the tag")
    @ApiResponse(responseCode = "404", description = "Tag not found or no tasks associated")
    public ResponseEntity<List<TaskResponseDTO>> getTasksForTag(
            @Parameter(description = "Name of the tag to filter tasks by")
            @PathVariable String tagName
    ) {
        List<TaskResponseDTO> tasks = taskRepository.findTasksByTagName(tagName)
                .stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList());

        if (tasks.isEmpty() && !tagService.existsByName(tagName)) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(tasks);
        }
    }
}
