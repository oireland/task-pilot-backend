package com.taskpilot.repository;

import com.taskpilot.model.TaskList;
import com.taskpilot.model.Todo;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class TaskListRepositoryTest {

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
    private TaskListRepository taskListRepository;

    // Tests for findByIdAndUser()
    @Test
    @DisplayName("findByIdAndUser() should return task when id exists and user matches")
    void findByIdAndUser_ShouldReturnTask_WhenIdExistsAndUserMatches() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = createAndPersistTask("Test Task", "Description", List.of("Item 1", "Item 2"), user);

        Optional<TaskList> result = taskListRepository.findByIdAndUser(taskList.getId(), user);

        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().getTitle());
        assertEquals(user.getId(), result.get().getUser().getId());
        assertEquals(2, result.get().getTodos().size());
        assertEquals("Item 1", result.get().getTodos().getFirst().getContent());
    }

    @Test
    @DisplayName("findByIdAndUser() should return empty when id exists but user does not match")
    void findByIdAndUser_ShouldReturnEmpty_WhenIdExistsButUserDoesNotMatch() {
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        TaskList taskList = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user1);

        Optional<TaskList> result = taskListRepository.findByIdAndUser(taskList.getId(), user2);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByIdAndUser() should return empty when id does not exist")
    void findByIdAndUser_ShouldReturnEmpty_WhenIdDoesNotExist() {
        User user = createAndPersistUser("john@example.com");

        Optional<TaskList> result = taskListRepository.findByIdAndUser(999L, user);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByIdAndUser() should handle null task id")
    void findByIdAndUser_ShouldHandleNull_TaskId() {
        User user = createAndPersistUser("john@example.com");

        assertDoesNotThrow(() -> {
            Optional<TaskList> result = taskListRepository.findByIdAndUser(null, user);
            assertTrue(result.isEmpty());
        });
    }

    @Test
    @DisplayName("findByIdAndUser() should handle null user")
    void findByIdAndUser_ShouldHandleNull_User() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);

        assertDoesNotThrow(() -> {
            Optional<TaskList> result = taskListRepository.findByIdAndUser(taskList.getId(), null);
            assertTrue(result.isEmpty());
        });
    }

    // Tests for deleteByIdAndUser()
    @Test
    @DisplayName("deleteByIdAndUser() should delete task when id exists and user matches")
    void deleteByIdAndUser_ShouldDeleteTask_WhenIdExistsAndUserMatches() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = createAndPersistTask("Task to Delete", "Description", List.of("Item 1"), user);
        Long taskId = taskList.getId();

        int deletedCount = taskListRepository.deleteByIdAndUser(taskId, user);
        entityManager.flush();
        entityManager.clear();

        assertEquals(1, deletedCount);
        Optional<TaskList> deletedTask = taskListRepository.findById(taskId);
        assertTrue(deletedTask.isEmpty());
    }

    @Test
    @DisplayName("deleteByIdAndUser() should return 0 when id exists but user does not match")
    void deleteByIdAndUser_ShouldReturn0_WhenIdExistsButUserDoesNotMatch() {
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        TaskList taskList = createAndPersistTask("Task to Delete", "Description", List.of("Item 1"), user1);

        int deletedCount = taskListRepository.deleteByIdAndUser(taskList.getId(), user2);
        entityManager.flush();

        assertEquals(0, deletedCount);
        Optional<TaskList> existingTask = taskListRepository.findById(taskList.getId());
        assertTrue(existingTask.isPresent());
    }

    @Test
    @DisplayName("deleteByIdAndUser() should return 0 when id does not exist")
    void deleteByIdAndUser_ShouldReturn0_WhenIdDoesNotExist() {
        User user = createAndPersistUser("john@example.com");

        int deletedCount = taskListRepository.deleteByIdAndUser(999L, user);

        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("deleteByIdAndUser() should handle null task id")
    void deleteByIdAndUser_ShouldHandleNull_TaskId() {
        User user = createAndPersistUser("john@example.com");

        assertDoesNotThrow(() -> {
            int deletedCount = taskListRepository.deleteByIdAndUser(null, user);
            assertEquals(0, deletedCount);
        });
    }

    @Test
    @DisplayName("deleteByIdAndUser() should handle null user")
    void deleteByIdAndUser_ShouldHandleNull_User() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);

        assertDoesNotThrow(() -> {
            int deletedCount = taskListRepository.deleteByIdAndUser(taskList.getId(), null);
            assertEquals(0, deletedCount);
        });
    }

    // Tests for deleteByIdInAndUser()
    @Test
    @DisplayName("deleteByIdInAndUser() should delete multiple tasks when ids exist and user matches")
    void deleteByIdInAndUser_ShouldDeleteMultipleTasks_WhenIdsExistAndUserMatches() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList1 = createAndPersistTask("Task 1", "Description 1", List.of("Item 1"), user);
        TaskList taskList2 = createAndPersistTask("Task 2", "Description 2", List.of("Item 2"), user);
        TaskList taskList3 = createAndPersistTask("Task 3", "Description 3", List.of("Item 3"), user);

        List<Long> taskIds = List.of(taskList1.getId(), taskList2.getId());

        int deletedCount = taskListRepository.deleteByIdInAndUser(taskIds, user);
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, deletedCount);
        assertTrue(taskListRepository.findById(taskList1.getId()).isEmpty());
        assertTrue(taskListRepository.findById(taskList2.getId()).isEmpty());
        assertTrue(taskListRepository.findById(taskList3.getId()).isPresent());
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should delete only tasks belonging to the user")
    void deleteByIdInAndUser_ShouldDeleteOnly_TasksBelongingToUser() {
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        TaskList taskList1 = createAndPersistTask("User1 Task 1", "Description", List.of("Item 1"), user1);
        TaskList taskList2 = createAndPersistTask("User1 Task 2", "Description", List.of("Item 2"), user1);
        TaskList taskList3 = createAndPersistTask("User2 Task", "Description", List.of("Item 3"), user2);

        List<Long> taskIds = List.of(taskList1.getId(), taskList2.getId(), taskList3.getId());

        int deletedCount = taskListRepository.deleteByIdInAndUser(taskIds, user1);
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, deletedCount);
        assertTrue(taskListRepository.findById(taskList1.getId()).isEmpty());
        assertTrue(taskListRepository.findById(taskList2.getId()).isEmpty());
        assertTrue(taskListRepository.findById(taskList3.getId()).isPresent());
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should return 0 when no ids match user's tasks")
    void deleteByIdInAndUser_ShouldReturn0_WhenNoIdsMatchUserTasks() {
        User user1 = createAndPersistUser("john@example.com");
        User user2 = createAndPersistUser("jane@example.com");
        TaskList taskList = createAndPersistTask("User1 Task", "Description", List.of("Item 1"), user1);

        List<Long> taskIds = List.of(taskList.getId());

        int deletedCount = taskListRepository.deleteByIdInAndUser(taskIds, user2);

        assertEquals(0, deletedCount);
        assertTrue(taskListRepository.findById(taskList.getId()).isPresent());
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should return 0 when ids list is empty")
    void deleteByIdInAndUser_ShouldReturn0_WhenIdsListIsEmpty() {
        User user = createAndPersistUser("john@example.com");

        int deletedCount = taskListRepository.deleteByIdInAndUser(Collections.emptyList(), user);

        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should handle non-existent ids gracefully")
    void deleteByIdInAndUser_ShouldHandleNonExistentIds_Gracefully() {
        User user = createAndPersistUser("john@example.com");
        List<Long> nonExistentIds = List.of(999L, 1000L);

        int deletedCount = taskListRepository.deleteByIdInAndUser(nonExistentIds, user);

        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("deleteByIdInAndUser() should handle null user")
    void deleteByIdInAndUser_ShouldHandleNull_User() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);
        List<Long> taskIds = Collections.singletonList(taskList.getId());

        assertDoesNotThrow(() -> {
            int deletedCount = taskListRepository.deleteByIdInAndUser(taskIds, null);
            assertEquals(0, deletedCount);
        });
    }

    // Tests for inherited JpaRepository methods
    @Test
    @DisplayName("save() should persist task with all properties")
    void save_ShouldPersistTask_WithAllProperties() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = new TaskList();
        taskList.setTitle("Test Task");
        taskList.setDescription("Test Description");
        taskList.setUser(user);
        attachTodos(taskList, List.of("Item 1", "Item 2", "Item 3"));

        TaskList savedTaskList = taskListRepository.save(taskList);

        assertNotNull(savedTaskList.getId());
        assertEquals("Test Task", savedTaskList.getTitle());
        assertEquals("Test Description", savedTaskList.getDescription());
        assertEquals(3, savedTaskList.getTodos().size());
        List<String> contents = savedTaskList.getTodos().stream().map(Todo::getContent).toList();
        assertTrue(contents.contains("Item 1"));
        assertTrue(contents.contains("Item 2"));
        assertTrue(contents.contains("Item 3"));
        assertEquals(user.getId(), savedTaskList.getUser().getId());
        assertNotNull(savedTaskList.getCreatedAt());
        assertNotNull(savedTaskList.getUpdatedAt());
    }

    @Test
    @DisplayName("save() should handle task with empty todos list")
    void save_ShouldHandleTask_WithEmptyItemsList() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = new TaskList();
        taskList.setTitle("Test Task");
        taskList.setDescription("Test Description");
        taskList.setUser(user);
        taskList.setTodos(new ArrayList<>());

        TaskList savedTaskList = taskListRepository.save(taskList);

        assertNotNull(savedTaskList.getId());
        assertTrue(savedTaskList.getTodos().isEmpty());
    }

    @Test
    @DisplayName("findById() should return task when id exists")
    void findById_ShouldReturnTask_WhenIdExists() {
        User user = createAndPersistUser("john@example.com");
        TaskList taskList = createAndPersistTask("Test Task", "Description", List.of("Item 1"), user);

        Optional<TaskList> result = taskListRepository.findById(taskList.getId());

        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().getTitle());
    }

    @Test
    @DisplayName("findById() should return empty when id does not exist")
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        Optional<TaskList> result = taskListRepository.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("count() should return correct number of tasks")
    void count_ShouldReturnCorrectNumber_OfTasks() {
        User user = createAndPersistUser("john@example.com");
        createAndPersistTask("Task 1", "Description 1", List.of("Item 1"), user);
        createAndPersistTask("Task 2", "Description 2", List.of("Item 2"), user);

        long count = taskListRepository.count();

        assertEquals(2, count);
    }

    @Test
    @DisplayName("findAll() should return all tasks")
    void findAll_ShouldReturnAllTasks() {
        User user = createAndPersistUser("john@example.com");
        createAndPersistTask("Task 1", "Description 1", List.of("Item 1"), user);
        createAndPersistTask("Task 2", "Description 2", List.of("Item 2"), user);

        List<TaskList> result = taskListRepository.findAll();

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

    private TaskList createAndPersistTask(String title, String description, List<String> items, User user) {
        TaskList taskList = new TaskList();
        taskList.setTitle(title);
        taskList.setDescription(description);
        taskList.setUser(user);
        attachTodos(taskList, items);
        return entityManager.persistAndFlush(taskList);
    }

    private void attachTodos(TaskList parent, List<String> contents) {
        List<Todo> todos = new ArrayList<>();
        for (String c : contents) {
            Todo t = new Todo();
            t.setContent(c);
            t.setChecked(false);
            t.setDeadline(null);
            t.setTaskList(parent);
            todos.add(t);
        }
        parent.setTodos(todos);
    }
}