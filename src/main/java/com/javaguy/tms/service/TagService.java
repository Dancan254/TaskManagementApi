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
        if (names == null || names.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalizedNames = names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (normalizedNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        Set<String> existingTagNames = existingTags.stream()
                .map(Tag::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<Tag> newTags = normalizedNames.stream()
                .filter(name -> !existingTagNames.contains(name))
                .map(Tag::new)
                .collect(Collectors.toSet());

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
            existingTags.addAll(newTags);
        }

        return existingTags;
    }

    public List<Tag> getAll() {
        return tagRepository.findAll();
    }

    @Transactional
    public boolean existsByName(String name) {
        return tagRepository.existsByName(name);
    }
}
