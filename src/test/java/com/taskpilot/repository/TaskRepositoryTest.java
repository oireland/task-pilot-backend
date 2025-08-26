package com.taskpilot.repository;

import com.taskpilot.model.Task;
import com.taskpilot.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class TaskRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    // Tests for findByIdAndUser()
    @Test
    @DisplayName("findByIdAndUser() should return task when id exists and user matches")
    void findByIdAndUser_ShouldReturnTask_WhenIdExistsAndUserMatches() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = createAndPersistTask("Test Task", "Description", List.of("Item 1", "Item 2"), user);

        // ACT
        Optional<Task> result = taskRepository.findByIdAndUser(task.getId(), user);

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().getTitle());
        assertEquals(user.getId(), result.get().getUser().getId());
    }

    @Test
    @DisplayName("findByIdAndUser() should return empty when id exists but user does not match")
    void findByIdAndUser_ShouldReturnEmpty_WhenIdExistsButUserDoesNotMatch() {
        // ARRANGE
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        Task task = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user1);

        // ACT
        Optional<Task> result = taskRepository.findByIdAndUser(task.getId(), user2);

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByIdAndUser() should return empty when id does not exist")
    void findByIdAndUser_ShouldReturnEmpty_WhenIdDoesNotExist() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");

        // ACT
        Optional<Task> result = taskRepository.findByIdAndUser(999L, user);

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByIdAndUser() should handle null task id")
    void findByIdAndUser_ShouldHandleNull_TaskId() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Optional<Task> result = taskRepository.findByIdAndUser(null, user);
            assertTrue(result.isEmpty());
        });
    }

    @Test
    @DisplayName("findByIdAndUser() should handle null user")
    void findByIdAndUser_ShouldHandleNull_User() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Optional<Task> result = taskRepository.findByIdAndUser(task.getId(), null);
            assertTrue(result.isEmpty());
        });
    }

    // Tests for deleteByIdAndUser()
    @Test
    @DisplayName("deleteByIdAndUser() should delete task when id exists and user matches")
    void deleteByIdAndUser_ShouldDeleteTask_WhenIdExistsAndUserMatches() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = createAndPersistTask("Task to Delete", "Description", List.of("Item 1"), user);
        Long taskId = task.getId();

        // ACT
        int deletedCount = taskRepository.deleteByIdAndUser(taskId, user);
        entityManager.flush();
        entityManager.clear();

        // ASSERT
        assertEquals(1, deletedCount);
        Optional<Task> deletedTask = taskRepository.findById(taskId);
        assertTrue(deletedTask.isEmpty());
    }

    @Test
    @DisplayName("deleteByIdAndUser() should return 0 when id exists but user does not match")
    void deleteByIdAndUser_ShouldReturn0_WhenIdExistsButUserDoesNotMatch() {
        // ARRANGE
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        Task task = createAndPersistTask("Task to Delete", "Description", List.of("Item 1"), user1);

        // ACT
        int deletedCount = taskRepository.deleteByIdAndUser(task.getId(), user2);
        entityManager.flush();

        // ASSERT
        assertEquals(0, deletedCount);
        // Verify task still exists
        Optional<Task> existingTask = taskRepository.findById(task.getId());
        assertTrue(existingTask.isPresent());
    }

    @Test
    @DisplayName("deleteByIdAndUser() should return 0 when id does not exist")
    void deleteByIdAndUser_ShouldReturn0_WhenIdDoesNotExist() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");

        // ACT
        int deletedCount = taskRepository.deleteByIdAndUser(999L, user);

        // ASSERT
        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("deleteByIdAndUser() should handle null task id")
    void deleteByIdAndUser_ShouldHandleNull_TaskId() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            int deletedCount = taskRepository.deleteByIdAndUser(null, user);
            assertEquals(0, deletedCount);
        });
    }

    @Test
    @DisplayName("deleteByIdAndUser() should handle null user")
    void deleteByIdAndUser_ShouldHandleNull_User() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            int deletedCount = taskRepository.deleteByIdAndUser(task.getId(), null);
            assertEquals(0, deletedCount);
        });
    }

    // Tests for deleteByIdInAndUser()
    @Test
    @DisplayName("deleteByIdInAndUser() should delete multiple tasks when ids exist and user matches")
    void deleteByIdInAndUser_ShouldDeleteMultipleTasks_WhenIdsExistAndUserMatches() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task1 = createAndPersistTask("Task 1", "Description 1", List.of("Item 1"), user);
        Task task2 = createAndPersistTask("Task 2", "Description 2", List.of("Item 2"), user);
        Task task3 = createAndPersistTask("Task 3", "Description 3", List.of("Item 3"), user);

        List<Long> taskIds = List.of(task1.getId(), task2.getId());

        // ACT
        int deletedCount = taskRepository.deleteByIdInAndUser(taskIds, user);
        entityManager.flush();
        entityManager.clear();

        // ASSERT
        assertEquals(2, deletedCount);
        assertTrue(taskRepository.findById(task1.getId()).isEmpty());
        assertTrue(taskRepository.findById(task2.getId()).isEmpty());
        assertTrue(taskRepository.findById(task3.getId()).isPresent()); // Should still exist
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should delete only tasks belonging to the user")
    void deleteByIdInAndUser_ShouldDeleteOnly_TasksBelongingToUser() {
        // ARRANGE
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        Task task1 = createAndPersistTask("User1 Task 1", "Description", List.of("Item 1"), user1);
        Task task2 = createAndPersistTask("User1 Task 2", "Description", List.of("Item 2"), user1);
        Task task3 = createAndPersistTask("User2 Task", "Description", List.of("Item 3"), user2);

        List<Long> taskIds = List.of(task1.getId(), task2.getId(), task3.getId());

        // ACT
        int deletedCount = taskRepository.deleteByIdInAndUser(taskIds, user1);
        entityManager.flush();
        entityManager.clear();

        // ASSERT
        assertEquals(2, deletedCount); // Only user1's tasks should be deleted
        assertTrue(taskRepository.findById(task1.getId()).isEmpty());
        assertTrue(taskRepository.findById(task2.getId()).isEmpty());
        assertTrue(taskRepository.findById(task3.getId()).isPresent()); // user2's task should remain
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should return 0 when no ids match user's tasks")
    void deleteByIdInAndUser_ShouldReturn0_WhenNoIdsMatchUserTasks() {
        // ARRANGE
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        Task task = createAndPersistTask("User1 Task", "Description", List.of("Item 1"), user1);

        List<Long> taskIds = List.of(task.getId());

        // ACT
        int deletedCount = taskRepository.deleteByIdInAndUser(taskIds, user2);

        // ASSERT
        assertEquals(0, deletedCount);
        assertTrue(taskRepository.findById(task.getId()).isPresent()); // Task should still exist
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should return 0 when ids list is empty")
    void deleteByIdInAndUser_ShouldReturn0_WhenIdsListIsEmpty() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");

        // ACT
        int deletedCount = taskRepository.deleteByIdInAndUser(Collections.emptyList(), user);

        // ASSERT
        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should handle non-existent ids gracefully")
    void deleteByIdInAndUser_ShouldHandleNonExistentIds_Gracefully() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        List<Long> nonExistentIds = List.of(999L, 1000L);

        // ACT
        int deletedCount = taskRepository.deleteByIdInAndUser(nonExistentIds, user);

        // ASSERT
        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should handle null user")
    void deleteByIdInAndUser_ShouldHandleNull_User() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);
        List<Long> taskIds = Collections.singletonList(task.getId());

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            int deletedCount = taskRepository.deleteByIdInAndUser(taskIds, null);
            assertEquals(0, deletedCount);
        });
    }

    // Tests for inherited JpaRepository methods
    @Test
    @DisplayName("save() should persist task with all properties")
    void save_ShouldPersistTask_WithAllProperties() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = new Task("Test Task", "Test Description", List.of("Item 1", "Item 2", "Item 3"), user);

        // ACT
        Task savedTask = taskRepository.save(task);

        // ASSERT
        assertNotNull(savedTask.getId());
        assertEquals("Test Task", savedTask.getTitle());
        assertEquals("Test Description", savedTask.getDescription());
        assertEquals(3, savedTask.getItems().size());
        assertTrue(savedTask.getItems().contains("Item 1"));
        assertTrue(savedTask.getItems().contains("Item 2"));
        assertTrue(savedTask.getItems().contains("Item 3"));
        assertEquals(user.getId(), savedTask.getUser().getId());
        assertNotNull(savedTask.getCreatedAt());
        assertNotNull(savedTask.getUpdatedAt());
    }

    @Test
    @DisplayName("save() should handle task with empty items list")
    void save_ShouldHandleTask_WithEmptyItemsList() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = new Task("Test Task", "Test Description", Collections.emptyList(), user);

        // ACT
        Task savedTask = taskRepository.save(task);

        // ASSERT
        assertNotNull(savedTask.getId());
        assertTrue(savedTask.getItems().isEmpty());
    }

    @Test
    @DisplayName("findById() should return task when id exists")
    void findById_ShouldReturnTask_WhenIdExists() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        Task task = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);

        // ACT
        Optional<Task> result = taskRepository.findById(task.getId());

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().getTitle());
    }

    @Test
    @DisplayName("findById() should return empty when id does not exist")
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        // ACT
        Optional<Task> result = taskRepository.findById(999L);

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("count() should return correct number of tasks")
    void count_ShouldReturnCorrectNumber_OfTasks() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        createAndPersistTask("Task 1", "Description 1", List.of("Item 1"), user);
        createAndPersistTask("Task 2", "Description 2", List.of("Item 2"), user);

        // ACT
        long count = taskRepository.count();

        // ASSERT
        assertEquals(2, count);
    }

    @Test
    @DisplayName("findAll() should return all tasks")
    void findAll_ShouldReturnAllTasks() {
        // ARRANGE
        User user = createAndPersistUser("john@example.com");
        createAndPersistTask("Task 1", "Description 1", List.of("Item 1"), user);
        createAndPersistTask("Task 2", "Description 2", List.of("Item 2"), user);

        // ACT
        List<Task> result = taskRepository.findAll();

        // ASSERT
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(task -> "Task 1".equals(task.getTitle())));
        assertTrue(result.stream().anyMatch(task -> "Task 2".equals(task.getTitle())));
    }

    // Helper methods
    private User createAndPersistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashedPassword");
        return entityManager.persistAndFlush(user);
    }

    private Task createAndPersistTask(String title, String description, List<String> items, User user) {
        Task task = new Task(title, description, items, user);
        return entityManager.persistAndFlush(task);
    }
}