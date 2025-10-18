package com.javaguy.tms.service;

import com.javaguy.tms.event.TaskCreatedEvent;
import com.javaguy.tms.event.TaskDeletedEvent;
import com.javaguy.tms.event.TaskUpdatedEvent;
import com.javaguy.tms.exception.OptimisticLockException;
import com.javaguy.tms.exception.ResourceNotFoundException;
import com.javaguy.tms.models.dto.TaskCreateDTO;
import com.javaguy.tms.models.dto.TaskResponseDTO;
import com.javaguy.tms.models.dto.TaskUpdateDTO;
import com.javaguy.tms.models.entity.Tag;
import com.javaguy.tms.models.entity.Task;
import com.javaguy.tms.models.enums.TaskStatus;
import com.javaguy.tms.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task Service Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TagService tagService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaskService taskService;

    private Task task1;
    private Task task2;
    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        tag1 = new Tag(1L, "Shopping", LocalDateTime.now(), new HashSet<>());
        tag2 = new Tag(2L, "Work", LocalDateTime.now(), new HashSet<>());

        task1 = new Task(1L, "Buy Groceries", "Milk, Bread", LocalDateTime.now().plusDays(1), TaskStatus.TODO, null, 0L, LocalDateTime.now(), LocalDateTime.now(), new HashSet<>(List.of(tag1)));
        task2 = new Task(2L, "Prepare Presentation", "For Q4 review", LocalDateTime.now().plusDays(3), TaskStatus.IN_PROGRESS, null, 0L, LocalDateTime.now(), LocalDateTime.now(), new HashSet<>(List.of(tag1, tag2)));
    }

    @Test
    @DisplayName("Should create a task and publish event")
    void shouldCreateTaskAndPublishEvent() {
        TaskCreateDTO createDTO = new TaskCreateDTO("New Task", "Description", LocalDateTime.now().plusDays(2), new HashSet<>(Collections.singletonList("New Tag")));
        Set<Tag> createdTags = new HashSet<>(Collections.singletonList(new Tag(3L, "New Tag", LocalDateTime.now(), new HashSet<>())));
        when(tagService.getOrCreateTagsByNames(anySet())).thenReturn(createdTags);
        when(taskRepository.save(any(Task.class))).thenReturn(task1);

        TaskResponseDTO result = taskService.createTask(createDTO);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Buy Groceries");
        verify(tagService, times(1)).getOrCreateTagsByNames(anySet());
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(eventPublisher, times(1)).publishEvent(any(TaskCreatedEvent.class));
    }

    @Test
    @DisplayName("Should get task by ID")
    void shouldGetTaskById() {
        when(taskRepository.findById(task1.getId())).thenReturn(Optional.of(task1));

        TaskResponseDTO result = taskService.getTaskById(task1.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(task1.getId());
        assertThat(result.getTitle()).isEqualTo(task1.getTitle());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if task not found by ID")
    void shouldThrowExceptionWhenTaskNotFound() {
        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(99L));
    }

    @Test
    @DisplayName("Should get all tasks")
    void shouldGetAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(task1, task2));

        List<TaskResponseDTO> result = taskService.getAllTasks();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskResponseDTO::getTitle).containsExactlyInAnyOrder("Buy Groceries", "Prepare Presentation");
    }

    @Test
    @DisplayName("Should get tasks by status")
    void shouldGetTasksByStatus() {
        when(taskRepository.findTasksByStatus(TaskStatus.TODO)).thenReturn(List.of(task1));

        List<TaskResponseDTO> result = taskService.getTasksByStatus(TaskStatus.TODO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Buy Groceries");
    }

    @Test
    @DisplayName("Should get tasks by tag")
    void shouldGetTasksByTag() {
        when(taskRepository.findTasksByTagName("Shopping")).thenReturn(List.of(task1, task2));

        List<TaskResponseDTO> result = taskService.getTasksByTag("Shopping");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskResponseDTO::getTitle).containsExactlyInAnyOrder("Buy Groceries", "Prepare Presentation");
    }

    @Test
    @DisplayName("Should get tasks by status and tag")
    void shouldGetTasksByStatusAndTag() {
        when(taskRepository.findTasksByStatusAndTagName(TaskStatus.TODO, "Shopping")).thenReturn(List.of(task1));

        List<TaskResponseDTO> result = taskService.getTasksByStatusAndTag(TaskStatus.TODO, "Shopping");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Buy Groceries");
    }

    @Test
    @DisplayName("Should update task without version (last-write-wins)")
    void shouldUpdateTaskWithoutVersion() {
        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated Description");
        updateDTO.setVersion(null);
        
        when(taskRepository.findById(task1.getId())).thenReturn(Optional.of(task1));
        when(taskRepository.save(any(Task.class))).thenReturn(task1);

        TaskResponseDTO result = taskService.updateTask(task1.getId(), updateDTO);

        assertThat(result).isNotNull();
        assertThat(task1.getTitle()).isEqualTo("Updated Title");
        assertThat(task1.getDescription()).isEqualTo("Updated Description");
        verify(taskRepository, times(1)).findById(task1.getId());
        verify(taskRepository, times(1)).save(task1);
        verify(eventPublisher, times(1)).publishEvent(any(TaskUpdatedEvent.class));
    }

    @Test
    @DisplayName("Should update task with correct version (optimistic locking success)")
    void shouldUpdateTaskWithCorrectVersion() {
        Task initialTask = new Task(1L, "Original", "Desc", LocalDateTime.now(), TaskStatus.TODO, null, 5L, LocalDateTime.now(), LocalDateTime.now(), new HashSet<>());
        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setVersion(5L);

        when(taskRepository.findById(initialTask.getId())).thenReturn(Optional.of(initialTask));
        when(taskRepository.save(any(Task.class))).thenReturn(initialTask);

        TaskResponseDTO result = taskService.updateTask(initialTask.getId(), updateDTO);

        assertThat(result).isNotNull();
        assertThat(initialTask.getTitle()).isEqualTo("Updated Title");
        verify(taskRepository, times(1)).findById(initialTask.getId());
        verify(taskRepository, times(1)).save(initialTask);
        verify(eventPublisher, times(1)).publishEvent(any(TaskUpdatedEvent.class));
    }

    @Test
    @DisplayName("Should throw OptimisticLockException with incorrect version")
    void shouldThrowOptimisticLockExceptionWithIncorrectVersion() {
        Task initialTask = new Task(1L, "Original", "Desc", LocalDateTime.now(), TaskStatus.TODO, null, 5L, LocalDateTime.now(), LocalDateTime.now(), new HashSet<>());
        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setVersion(4L);

        when(taskRepository.findById(initialTask.getId())).thenReturn(Optional.of(initialTask));

        assertThrows(OptimisticLockException.class, () -> taskService.updateTask(initialTask.getId(), updateDTO));
        verify(taskRepository, never()).save(any(Task.class));
        verify(eventPublisher, never()).publishEvent(any(TaskUpdatedEvent.class));
    }

    @Test
    @DisplayName("Should delete task and publish event")
    void shouldDeleteTaskAndPublishEvent() {
        when(taskRepository.findById(task1.getId())).thenReturn(Optional.of(task1));
        doNothing().when(taskRepository).delete(task1);

        taskService.deleteTask(task1.getId());

        verify(taskRepository, times(1)).findById(task1.getId());
        verify(taskRepository, times(1)).delete(task1);
        verify(eventPublisher, times(1)).publishEvent(any(TaskDeletedEvent.class));
        assertThat(task1.getTags()).isEmpty();
    }
}
