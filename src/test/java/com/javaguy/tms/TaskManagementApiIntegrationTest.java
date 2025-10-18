package com.javaguy.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaguy.tms.models.dto.TaskCreateDTO;
import com.javaguy.tms.models.dto.TaskResponseDTO;
import com.javaguy.tms.models.dto.TaskUpdateDTO;
import com.javaguy.tms.models.enums.TaskStatus;
import com.javaguy.tms.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Full Integration Tests for Task Management API")
class TaskManagementApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    private Long taskId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("1. Should create a task and return 201 Created")
    void shouldCreateTask() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO("Buy Milk", "Remember to get organic milk", LocalDateTime.now().plusDays(2), Set.of("Shopping", "Home"));

        String response = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Buy Milk")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        TaskResponseDTO createdTask = objectMapper.readValue(response, TaskResponseDTO.class);
        taskId = createdTask.getId();
    }

    @Test
    @Order(2)
    @DisplayName("2. Should retrieve all tasks including the created one")
    void shouldGetAllTasks() throws Exception {
        if (taskId == null) {
            shouldCreateTask();
        }

        mockMvc.perform(get("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Buy Milk")));
    }

    @Test
    @Order(3)
    @DisplayName("3. Should retrieve tasks by status")
    void shouldGetTasksByStatus() throws Exception {
        if (taskId == null) {
            shouldCreateTask();
        }

        mockMvc.perform(get("/api/v1/tasks/status/TODO")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Buy Milk")));
    }

    @Test
    @Order(4)
    @DisplayName("4. Should update the created task with a new status and version")
    void shouldUpdateTask() throws Exception {
        if (taskId == null) {
            shouldCreateTask();
        }

        String getResponse = mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TaskResponseDTO fetchedTask = objectMapper.readValue(getResponse, TaskResponseDTO.class);

        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setStatus(TaskStatus.COMPLETED);
        updateDTO.setTitle("Milk Bought!");
        updateDTO.setVersion(fetchedTask.getVersion());

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Milk Bought!")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @Order(5)
    @DisplayName("5. Should delete the task")
    void shouldDeleteTask() throws Exception {
        if (taskId == null) {
            shouldCreateTask();
        }

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
