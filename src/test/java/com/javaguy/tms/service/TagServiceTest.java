package com.javaguy.tms.service;

import com.javaguy.tms.models.entity.Tag;
import com.javaguy.tms.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tag Service Tests")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        tag1 = new Tag(1L, "Personal", LocalDateTime.now(), new HashSet<>());
        tag2 = new Tag(2L, "Work", LocalDateTime.now(), new HashSet<>());
    }

    @Test
    @DisplayName("Should get existing tags by names and create new ones")
    void shouldGetOrCreateTagsByNames() {
        Set<String> tagNames = new HashSet<>();
        tagNames.add("Personal");
        tagNames.add("new_tag");

        Tag existingTag = new Tag(1L, "personal", LocalDateTime.now(), new HashSet<>());
        Set<Tag> existingTagsSet = new HashSet<>();
        existingTagsSet.add(existingTag);
        when(tagRepository.findByNameIn(any())).thenReturn(existingTagsSet);

        when(tagRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Tag> tags = invocation.getArgument(0);
            List<Tag> savedTags = new ArrayList<>();
            tags.forEach(tag -> {
                tag.setId(3L);
                savedTags.add(tag);
            });
            return savedTags;
        });

        Set<Tag> result = tagService.getOrCreateTagsByNames(tagNames);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Tag::getName)
                .containsExactlyInAnyOrder("personal", "new_tag");

        verify(tagRepository, times(1)).findByNameIn(any());
        verify(tagRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Should return true if tag exists by name")
    void shouldReturnTrueIfTagExistsByName() {
        when(tagRepository.existsByName(anyString())).thenReturn(true);

        boolean exists = tagService.existsByName("Personal");

        assertThat(exists).isTrue();
        verify(tagRepository, times(1)).existsByName(anyString());
    }

    @Test
    @DisplayName("Should return false if tag does not exist by name")
    void shouldReturnFalseIfTagDoesNotExistByName() {
        when(tagRepository.existsByName(anyString())).thenReturn(false);

        boolean exists = tagService.existsByName("NonExistent");

        assertThat(exists).isFalse();
        verify(tagRepository, times(1)).existsByName(anyString());
    }

    @Test
    @DisplayName("Should get all tags")
    void shouldGetAllTags() {
        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));

        List<Tag> result = tagService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Tag::getName).containsExactlyInAnyOrder("Personal", "Work");
        verify(tagRepository, times(1)).findAll();
    }
}
