package com.javaguy.tms.repository;

import com.javaguy.tms.models.entity.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tag Repository Tests")
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        tagRepository.deleteAll();
        entityManager.clear();

        tag1 = new Tag("Personal");
        tag2 = new Tag("Work");

        entityManager.persist(tag1);
        entityManager.persist(tag2);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find tag by ID")
    void shouldFindTagById() {
        Optional<Tag> foundTag = tagRepository.findById(tag1.getId());
        assertThat(foundTag).isPresent();
        assertThat(foundTag.get().getName()).isEqualTo("Personal");
    }

    @Test
    @DisplayName("Should find tag by name")
    void shouldFindTagByName() {
        Optional<Tag> foundTag = tagRepository.findByName("Work");
        assertThat(foundTag).isPresent();
        assertThat(foundTag.get().getName()).isEqualTo("Work");
    }

    @Test
    @DisplayName("Should return true if tag exists by name")
    void shouldReturnTrueIfTagExistsByName() {
        boolean exists = tagRepository.existsByName("Personal");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false if tag does not exist by name")
    void shouldReturnFalseIfTagDoesNotExistByName() {
        boolean exists = tagRepository.existsByName("NonExistent");
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should save a new tag")
    void shouldSaveNewTag() {
        Tag newTag = new Tag("Learning");
        Tag savedTag = tagRepository.save(newTag);
        assertThat(savedTag.getId()).isNotNull();
        assertThat(tagRepository.findByName("Learning")).isPresent();
    }

    @Test
    @DisplayName("Should delete a tag by ID")
    void shouldDeleteTagById() {
        tagRepository.deleteById(tag1.getId());
        assertThat(tagRepository.findById(tag1.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should find all tags")
    void shouldFindAllTags() {
        List<Tag> tags = tagRepository.findAll();
        assertThat(tags).hasSize(2);
        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("Personal", "Work");
    }
}
