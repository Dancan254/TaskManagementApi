package com.javaguy.tms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaguy.tms.exception.ResourceNotFoundException;
import com.javaguy.tms.models.dto.TaskCreateDTO;
import com.javaguy.tms.models.dto.TaskResponseDTO;
import com.javaguy.tms.models.dto.TaskUpdateDTO;
import com.javaguy.tms.models.enums.TaskStatus;
import com.javaguy.tms.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TaskController.class)
@DisplayName("Task Controller Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    private TaskResponseDTO createTaskResponseDTO(Long id, String title, TaskStatus status, Set<String> tags) {
        return TaskResponseDTO.builder()
                .id(id)
                .title(title)
                .description("Desc for " + title)
                .dueDate(LocalDateTime.now().plusDays(1))
                .status(status)
                .tags(tags)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Should create a new task")
    void shouldCreateTask() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO("Test Task", "Description", LocalDateTime.now().plusDays(1), Collections.singleton("Tag1"));
        TaskResponseDTO responseDTO = createTaskResponseDTO(1L, "Test Task", TaskStatus.TODO, Collections.singleton("Tag1"));

        when(taskService.createTask(any(TaskCreateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Task")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - Should get all tasks")
    void shouldGetAllTasks() throws Exception {
        List<TaskResponseDTO> allTasks = List.of(
                createTaskResponseDTO(1L, "Task 1", TaskStatus.TODO, Collections.singleton("Home")),
                createTaskResponseDTO(2L, "Task 2", TaskStatus.IN_PROGRESS, Collections.singleton("Work"))
        );
        when(taskService.getAllTasks()).thenReturn(allTasks);

        mockMvc.perform(get("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Task 1")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/status/{status} - Should get tasks by status")
    void shouldGetTasksByStatus() throws Exception {
        List<TaskResponseDTO> todoTasks = List.of(
                createTaskResponseDTO(1L, "Task A", TaskStatus.TODO, Collections.singleton("Study"))
        );
        when(taskService.getTasksByStatus(TaskStatus.TODO)).thenReturn(todoTasks);

        mockMvc.perform(get("/api/v1/tasks/status/TODO")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task A")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/tag/{tagName} - Should get tasks by tag")
    void shouldGetTasksByTag() throws Exception {
        List<TaskResponseDTO> shoppingTasks = List.of(
                createTaskResponseDTO(1L, "Buy Milk", TaskStatus.TODO, Collections.singleton("Shopping"))
        );
        when(taskService.getTasksByTag("Shopping")).thenReturn(shoppingTasks);

        mockMvc.perform(get("/api/v1/tasks/tag/Shopping")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Buy Milk")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/filter?status={status}&tag={tag} - Should get tasks by status and tag")
    void shouldGetTasksByStatusAndTag() throws Exception {
        List<TaskResponseDTO> filteredTasks = List.of(
                createTaskResponseDTO(1L, "Buy Eggs", TaskStatus.TODO, Collections.singleton("Groceries"))
        );
        when(taskService.getTasksByStatusAndTag(TaskStatus.TODO, "Groceries")).thenReturn(filteredTasks);

        mockMvc.perform(get("/api/v1/tasks/filter")
                        .param("status", "TODO")
                        .param("tag", "Groceries")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Buy Eggs")));
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} - Should update an existing task")
    void shouldUpdateTask() throws Exception {
        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setTitle("Updated Task");
        updateDTO.setStatus(TaskStatus.COMPLETED);

        TaskResponseDTO responseDTO = createTaskResponseDTO(1L, "Updated Task", TaskStatus.COMPLETED, Collections.emptySet());
        when(taskService.updateTask(eq(1L), any(TaskUpdateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Task")));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} - Should delete a task")
    void shouldDeleteTask() throws Exception {
        doNothing().when(taskService).deleteTask(anyLong());

        mockMvc.perform(delete("/api/v1/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} - Should return 404 if task not found for deletion")
    void shouldReturnNotFoundWhenDeletingNonExistentTask() throws Exception {
        doThrow(new ResourceNotFoundException("Task not found")).when(taskService).deleteTask(anyLong());

        mockMvc.perform(delete("/api/v1/tasks/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Task not found")));
    }
}
