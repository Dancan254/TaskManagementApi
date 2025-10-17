package com.javaguy.tms.repository;


import com.javaguy.tms.models.entity.Task;
import com.javaguy.tms.models.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findTasksByStatus(TaskStatus status);

    @Query("SELECT t FROM Task t JOIN FETCH t.tags tag WHERE LOWER(tag.name) = LOWER(:tagName)")
    List<Task> findTasksByTagName(String tagName);

    @Query("""
        SELECT DISTINCT t FROM Task t
        LEFT JOIN FETCH t.tags tag
        WHERE (:status IS NULL OR t.status = :status)
        AND (:tagName IS NULL OR LOWER(tag.name) = LOWER(:tagName))
        ORDER BY t.createdAt DESC""")
    List<Task> findTasksByStatusAndTagName(TaskStatus status, String tagName);
}
