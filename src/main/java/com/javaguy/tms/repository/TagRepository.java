package com.javaguy.tms.repository;

import com.javaguy.tms.models.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    boolean existsByName(String name);
    Set<Tag> findByNameIn(Set<String> names);
}
