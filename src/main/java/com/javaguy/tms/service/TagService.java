package com.javaguy.tms.service;

import com.javaguy.tms.models.entity.Tag;
import com.javaguy.tms.repository.TagRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Set<Tag> getOrCreateTagsByNames(Set<String> names) {
        if (names == null || names.isEmpty()) return new HashSet<>();
        Set<String> normalized = names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Map<String, Tag> found = tagRepository.findAll().stream()
                .filter(t -> normalized.contains(t.getName().toLowerCase()))
                .collect(Collectors.toMap(t -> t.getName().toLowerCase(), t -> t));
        Set<Tag> result = new HashSet<>();
        for (String name : normalized) {
            Tag tag = found.get(name);
            if (tag == null) {
                tag = new Tag(name);
                tagRepository.save(tag);
            }
            result.add(tag);
        }
        return result;
    }

    public List<Tag> getAll() {
        return tagRepository.findAll();
    }
}
