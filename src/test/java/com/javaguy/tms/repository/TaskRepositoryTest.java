package com.javaguy.tms.repository;

import com.javaguy.tms.models.entity.Tag;
import com.javaguy.tms.models.entity.Task;
import com.javaguy.tms.models.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Task Repository Tests")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Task task1;
    private Task task2;
    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        entityManager.clear();

        tag1 = new Tag("Shopping");
        tag2 = new Tag("Work");
        entityManager.persist(tag1);
        entityManager.persist(tag2);
        entityManager.flush();

        Set<Tag> tags1 = new HashSet<>();
        tags1.add(tag1);

        Set<Tag> tags2 = new HashSet<>();
        tags2.add(tag2);
        tags2.add(tag1);

        task1 = new Task(null, "Buy Groceries", "Milk, Bread", LocalDateTime.now().plusDays(1), TaskStatus.TODO, null, 0L, null, null, tags1);
        task2 = new Task(null, "Prepare Presentation", "For Q4 review", LocalDateTime.now().plusDays(3), TaskStatus.IN_PROGRESS, null, 0L, null, null, tags2);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find task by ID")
    void shouldFindTaskById() {
        Optional<Task> foundTask = taskRepository.findById(task1.getId());
        assertThat(foundTask).isPresent();
        assertThat(foundTask.get().getTitle()).isEqualTo("Buy Groceries");
        assertThat(foundTask.get().getTags()).containsExactlyInAnyOrder(tag1);
    }

    @Test
    @DisplayName("Should find tasks by status")
    void shouldFindTasksByStatus() {
        List<Task> todoTasks = taskRepository.findTasksByStatus(TaskStatus.TODO);
        assertThat(todoTasks).hasSize(1);
        assertThat(todoTasks.get(0).getTitle()).isEqualTo("Buy Groceries");
    }

    @Test
    @DisplayName("Should find tasks by tag name (case-insensitive)")
    void shouldFindTasksByTagNameCaseInsensitive() {
        List<Task> shoppingTasks = taskRepository.findTasksByTagName("shopping");
        assertThat(shoppingTasks).hasSize(2);
        assertThat(shoppingTasks).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Buy Groceries", "Prepare Presentation");
    }

    @Test
    @DisplayName("Should find tasks by status and tag name")
    void shouldFindTasksByStatusAndTagName() {
        List<Task> filteredTasks = taskRepository.findTasksByStatusAndTagName(TaskStatus.TODO, "Shopping");
        assertThat(filteredTasks).hasSize(1);
        assertThat(filteredTasks.get(0).getTitle()).isEqualTo("Buy Groceries");
    }

    @Test
    @DisplayName("Should save a new task")
    void shouldSaveNewTask() {
        Task newTask = new Task(null, "New Task", "Description", LocalDateTime.now().plusDays(5), TaskStatus.TODO, null, 0L, null, null, new HashSet<>());
        Task savedTask = taskRepository.save(newTask);
        assertThat(savedTask.getId()).isNotNull();
        assertThat(taskRepository.findById(savedTask.getId())).isPresent();
    }

    @Test
    @DisplayName("Should update an existing task")
    void shouldUpdateExistingTask() {
        task1.setTitle("Updated Groceries List");
        task1.setStatus(TaskStatus.COMPLETED);
        Task updatedTask = taskRepository.save(task1);
        assertThat(updatedTask.getTitle()).isEqualTo("Updated Groceries List");
        assertThat(updatedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(taskRepository.findById(task1.getId()).get().getTitle()).isEqualTo("Updated Groceries List");
    }

    @Test
    @DisplayName("Should delete a task by ID")
    void shouldDeleteTaskById() {
        taskRepository.deleteById(task1.getId());
        assertThat(taskRepository.findById(task1.getId())).isEmpty();
    }
}