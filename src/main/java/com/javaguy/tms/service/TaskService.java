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
import com.javaguy.tms.util.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TaskResponseDTO createTask(TaskCreateDTO dto) {
        log.info("Creating task: {}", dto.getTitle());

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());
        task.setStatus(TaskStatus.TODO);

        Set<Tag> tags = tagService.getOrCreateTagsByNames(dto.getTags());
        task.setTags(tags);

        Task saved = taskRepository.save(task);

        log.info("Task created with ID: {}", saved.getId());

        eventPublisher.publishEvent(new TaskCreatedEvent(saved));

        return TaskMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(Long id) {
        log.debug("Fetching task with ID: {}", id);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));

        return TaskMapper.toDto(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getAllTasks() {
        log.debug("Fetching all tasks");
        return taskRepository.findAll().stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByStatus(TaskStatus status) {
        log.debug("Fetching tasks by status: {}", status);
        return taskRepository.findTasksByStatus(status).stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByTag(String tagName) {
        log.debug("Fetching tasks by tag: {}", tagName);
        return taskRepository.findTasksByTagName(tagName).stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByStatusAndTag(TaskStatus status, String tagName) {
        log.debug("Fetching tasks by status: {} and tag: {}", status, tagName);
        return taskRepository.findTasksByStatusAndTagName(status, tagName).stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskUpdateDTO dto) {
        log.info("Updating task with ID: {}", id);

        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));

        // Optimistic locking check (only if version is provided)
        if (dto.getVersion() != null) {
            if (!Objects.equals(existing.getVersion(), dto.getVersion())) {
                throw new OptimisticLockException(
                        "Version mismatch. Expected " + existing.getVersion() + ", got " + dto.getVersion()
                );
            }
        }

        // Update fields if provided
        if (dto.getTitle() != null) {
            existing.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }

        if (dto.getDueDate() != null) {
            existing.setDueDate(dto.getDueDate());
        }

        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }

        // Update tags if provided
        if (dto.getTags() != null) {
            Set<Tag> tags = tagService.getOrCreateTagsByNames(dto.getTags());
            existing.getTags().clear();
            existing.getTags().addAll(tags);
        }

        Task saved = taskRepository.save(existing);

        log.info("Task {} updated successfully", id);

        eventPublisher.publishEvent(new TaskUpdatedEvent(saved));

        return TaskMapper.toDto(saved);
    }

    @Transactional
    public void deleteTask(Long id) {
        log.info("Deleting task with ID: {}", id);

        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        existing.getTags().clear();
        taskRepository.delete(existing);

        log.info("Task {} deleted successfully", id);

        eventPublisher.publishEvent(new TaskDeletedEvent(existing));
    }
}