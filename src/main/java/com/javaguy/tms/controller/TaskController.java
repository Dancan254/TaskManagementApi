package com.javaguy.tms.controller;


import com.javaguy.tms.models.dto.TaskCreateDTO;
import com.javaguy.tms.models.dto.TaskResponseDTO;
import com.javaguy.tms.models.dto.TaskUpdateDTO;
import com.javaguy.tms.models.enums.TaskStatus;
import com.javaguy.tms.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Management", description = "APIs for managing tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(
            summary = "Create a new task",
            description = "Creates a new task with optional tags. Tags are auto-created if they don't exist."
    )
    @ApiResponse(responseCode = "201", description = "Task created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<TaskResponseDTO> createTask(@Valid @RequestBody TaskCreateDTO dto) {
        log.info("Creating task with title: {}", dto.getTitle());
        return new ResponseEntity<>(taskService.createTask(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(
            summary = "List all tasks",
            description = "Retrieves all tasks with optional filters by status and tag"
    )
    public ResponseEntity<List<TaskResponseDTO>> getAllTasks() {
        log.info("Fetching all tasks without filters");
        return new ResponseEntity<>(taskService.getAllTasks(), HttpStatus.OK);
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "List tasks by status",
            description = "Retrieves tasks filtered by a specific status"
    )
    public ResponseEntity<List<TaskResponseDTO>> getTasksByStatus(
            @Parameter(description = "Filter by task status")
            @PathVariable TaskStatus status
    ) {
        log.info("Fetching tasks with status: {}", status);
        return new ResponseEntity<>(taskService.getTasksByStatus(status), HttpStatus.OK);
    }

    @GetMapping("/tag/{tagName}")
    @Operation(
            summary = "List tasks by tag name",
            description = "Retrieves tasks filtered by a specific tag name"
    )
    public ResponseEntity<List<TaskResponseDTO>> getTasksByTag(
            @Parameter(description = "Filter by tag name")
            @PathVariable String tagName
    ) {
        log.info("Fetching tasks with tag: {}", tagName);
        return new ResponseEntity<>(taskService.getTasksByTag(tagName), HttpStatus.OK);
    }

    @GetMapping("/filter")
    @Operation(
            summary = "List tasks by status and tag",
            description = "Retrieves tasks filtered by both status and tag"
    )
    public ResponseEntity<List<TaskResponseDTO>> getTasksByStatusAndTag(
            @Parameter(description = "Filter by task status")
            @RequestParam TaskStatus status,
            @Parameter(description = "Filter by tag name")
            @RequestParam String tag
    ) {
        log.info("Fetching tasks with status: {} and tag: {}", status, tag);
        return new ResponseEntity<>(taskService.getTasksByStatusAndTag(status, tag), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<TaskResponseDTO> getTask(@PathVariable Long id) {
        log.info("Fetching task with id: {}", id);
        return new ResponseEntity<>(taskService.getTaskById(id), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update task",
            description = "Updates task fields. Only provided fields are updated."
    )
    @ApiResponse(responseCode = "200", description = "Task updated successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "409", description = "Concurrent modification conflict")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateDTO dto
    ) {
        log.info("Updating task with id: {}", id);
        return new ResponseEntity<>(taskService.updateTask(id, dto),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete task",
            description = "Deletes a task and removes its calendar event. Tags are preserved."
    )
    @ApiResponse(responseCode = "204", description = "Task deleted successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public void deleteTask(@PathVariable Long id) {
        log.info("Deleting task with id: {}", id);
        taskService.deleteTask(id);
    }
}