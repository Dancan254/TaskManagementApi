package com.javaguy.tms.controller;

import com.javaguy.tms.models.entity.Tag;
import com.javaguy.tms.models.entity.Task;
import com.javaguy.tms.models.enums.TaskStatus;
import com.javaguy.tms.repository.TaskRepository;
import com.javaguy.tms.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TagController.class)
@DisplayName("Tag Controller Tests")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private TaskRepository taskRepository;

    private Task createTaskEntity(Long id, String title) {
        return new Task(id, title, "desc", LocalDateTime.now(), TaskStatus.TODO, null, 0L, LocalDateTime.now(), LocalDateTime.now(), new HashSet<>());
    }

    @Test
    @DisplayName("GET /api/tags - Should list all tags with task counts")
    void shouldListAllTagsWithTaskCounts() throws Exception {
        // Arrange
        Tag tagEntity1 = new Tag(1L, "Work", LocalDateTime.now(), new HashSet<>());
        Tag tagEntity2 = new Tag(2L, "Personal", LocalDateTime.now(), new HashSet<>());

        when(tagService.getAll()).thenReturn(List.of(tagEntity1, tagEntity2));

        // Mock the TaskRepository calls for task counts
        when(taskRepository.findTasksByTagName("Work")).thenReturn(Collections.nCopies(5, createTaskEntity(10L, "Work Task")));
        when(taskRepository.findTasksByTagName("Personal")).thenReturn(Collections.nCopies(3, createTaskEntity(20L, "Personal Task")));

        // Act & Assert
        mockMvc.perform(get("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Work")))
                .andExpect(jsonPath("$[0].taskCount", is(5)))
                .andExpect(jsonPath("$[1].name", is("Personal")))
                .andExpect(jsonPath("$[1].taskCount", is(3)));
    }

    @Test
    @DisplayName("GET /api/tags/{tagName}/tasks - Should get tasks for a specific tag")
    void shouldGetTasksForTag() throws Exception {
        // Arrange
        List<Task> tasksForTag = List.of(
                createTaskEntity(1L, "Task A"),
                createTaskEntity(2L, "Task B")
        );
        when(tagService.existsByName("Work")).thenReturn(true);
        when(taskRepository.findTasksByTagName("Work")).thenReturn(tasksForTag);

        // Act & Assert
        mockMvc.perform(get("/api/tags/{tagName}/tasks", "Work")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Task A")));
    }

    @Test
    @DisplayName("GET /api/tags/{tagName}/tasks - Should return 404 if tag does not exist")
    void shouldReturn404IfTagDoesNotExist() throws Exception {
        // Arrange
        when(tagService.existsByName(anyString())).thenReturn(false);
        when(taskRepository.findTasksByTagName(anyString())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tags/{tagName}/tasks", "NonExistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tags/{tagName}/tasks - Should return 200 with empty list if tag exists but no tasks")
    void shouldReturn200WithEmptyListIfTagExistsButNoTasks() throws Exception {
        // Arrange
        when(tagService.existsByName("EmptyTag")).thenReturn(true);
        when(taskRepository.findTasksByTagName("EmptyTag")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tags/{tagName}/tasks", "EmptyTag")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
