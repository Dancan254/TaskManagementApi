package com.javaguy.tms.controller;

import com.javaguy.tms.models.dto.TagResponseDTO;
import com.javaguy.tms.models.dto.TaskResponseDTO;
import com.javaguy.tms.models.entity.Tag;
import com.javaguy.tms.repository.TaskRepository;
import com.javaguy.tms.service.TagService;
import com.javaguy.tms.util.TaskMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;
    private final TaskRepository taskRepository;

    public TagController(TagService tagService, TaskRepository taskRepository) {
        this.tagService = tagService;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public List<TagResponseDTO> list() {
        List<Tag> tags = tagService.getAll();
        return tags.stream()
                .map(t -> TagResponseDTO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .taskCount(taskRepository.findTasksByTagName(t.getName()).size())
                        .build())
                .collect(Collectors.toList());
    }

    @GetMapping("/{tagName}/tasks")
    public List<TaskResponseDTO> getTasksForTag(@PathVariable String tagName) {
        return taskRepository.findTasksByTagName(tagName)
                .stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList());
    }
}
