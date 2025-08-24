package com.taskpilot.service;

import com.taskpilot.dto.task.CreateTaskDTO;
import com.taskpilot.dto.task.ExtractedTaskListDTO;
import com.taskpilot.dto.task.TaskDTO;
import com.taskpilot.dto.task.UpdateTaskDTO;
import com.taskpilot.model.Task;
import com.taskpilot.model.User;
import com.taskpilot.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        // Create a reusable User and Task for our tests.
        testUser = new User("user@example.com", "password");
        testUser.setId(1L);

        testTask = new Task("Test Title", "Test Description", List.of("Item 1"), testUser);
        testTask.setId(100L);
        testTask.setCreatedAt(LocalDateTime.now().minusDays(1));
        testTask.setUpdatedAt(LocalDateTime.now());
    }

    // --- READ Operations ---

    @Test
    @DisplayName("getTaskByIdForUser() should return TaskDTO if found and owned by user")
    void getTaskByIdForUser_shouldReturnDtoWhenFound() {
        // ARRANGE: Mock the repository to return our test task.
        when(taskRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testTask));

        // ACT: Call the service method.
        Optional<TaskDTO> result = taskService.getTaskByIdForUser(100L, testUser);

        // ASSERT: Check that the result is present and contains the correct data.
        assertTrue(result.isPresent());
        assertEquals(testTask.getTitle(), result.get().title());
        assertEquals(testTask.getId(), result.get().id());
    }

    @Test
    @DisplayName("getTaskByIdForUser() should return empty Optional if not found")
    void getTaskByIdForUser_shouldReturnEmptyWhenNotFound() {
        // ARRANGE: Mock the repository to return an empty Optional.
        when(taskRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        // ACT: Call the service method.
        Optional<TaskDTO> result = taskService.getTaskByIdForUser(999L, testUser);

        // ASSERT: Check that the result is empty.
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTasksForUser() should return a page of tasks")
    void getTasksForUser_shouldReturnPageOfTasks() {
        // ARRANGE
        Pageable pageable = Pageable.unpaged();
        Page<Task> taskPage = new PageImpl<>(List.of(testTask));
        // We use any(Specification.class) because building the exact Specification object is complex and not necessary for this unit test.
        when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

        // ACT
        Page<TaskDTO> resultPage = taskService.getTasksForUser(testUser, null, pageable);

        // ASSERT
        assertEquals(1, resultPage.getTotalElements());
        assertEquals("Test Title", resultPage.getContent().getFirst().title());
    }

    // --- CREATE Operations ---

    @Test
    @DisplayName("createTask(ExtractedTaskListDTO) should save a new task")
    void createTaskFromExtractedDTO_shouldSaveNewTask() {
        // ARRANGE
        ExtractedTaskListDTO dto = new ExtractedTaskListDTO("Extracted Title", "Extracted Desc", List.of("Task A"));
        when(taskRepository.save(any(Task.class))).thenReturn(new Task());

        // ACT
        taskService.createTask(dto, testUser);

        // ASSERT & VERIFY
        // Use an ArgumentCaptor to capture the Task object that was passed to the save method.
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        Task savedTask = taskCaptor.getValue();
        assertEquals("Extracted Title", savedTask.getTitle());
        assertEquals(testUser, savedTask.getUser());
    }

    @Test
    @DisplayName("createTask(CreateTaskDTO) should save and return a TaskDTO")
    void createTaskFromCreateDTO_shouldSaveAndReturnDTO() {
        // ARRANGE
        CreateTaskDTO dto = new CreateTaskDTO("Manual Title", "Manual Desc", List.of("Manual Item"));
        // When save is called, return our pre-configured testTask to simulate the DB assigning an ID and timestamps.
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // ACT
        TaskDTO resultDTO = taskService.createTask(dto, testUser);

        // ASSERT
        assertNotNull(resultDTO);
        assertEquals(testTask.getId(), resultDTO.id());
        assertEquals("Test Title", resultDTO.title()); // The title from the returned testTask
    }

    // --- UPDATE Operation ---

    @Test
    @DisplayName("updateTask() should update and save the task if found")
    void updateTask_shouldUpdateIfFound() {
        // ARRANGE
        UpdateTaskDTO updateDto = new UpdateTaskDTO("Updated Title", "Updated Desc", List.of("Updated Item"));
        when(taskRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testTask));
        // When save is called, return the same task that was passed in.
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        Optional<TaskDTO> result = taskService.updateTask(100L, updateDto, testUser);

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Updated Title", result.get().title());
        assertEquals("Updated Desc", result.get().description());

        // VERIFY that the save method was called.
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("updateTask() should return empty Optional if task is not found")
    void updateTask_shouldReturnEmptyIfNotFound() {
        // ARRANGE
        UpdateTaskDTO updateDto = new UpdateTaskDTO("Title", "Desc", List.of());
        when(taskRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        // ACT
        Optional<TaskDTO> result = taskService.updateTask(999L, updateDto, testUser);

        // ASSERT
        assertTrue(result.isEmpty());
        // VERIFY that save was never called.
        verify(taskRepository, never()).save(any(Task.class));
    }


    // --- DELETE Operations ---

    @Test
    @DisplayName("deleteTask() should return true when a task is deleted")
    void deleteTask_shouldReturnTrueWhenDeleted() {
        // ARRANGE: Mock the repository to return 1, indicating one row was deleted.
        when(taskRepository.deleteByIdAndUser(100L, testUser)).thenReturn(1);

        // ACT
        boolean result = taskService.deleteTask(100L, testUser);

        // ASSERT
        assertTrue(result);
        // VERIFY that the correct delete method was called on the repository.
        verify(taskRepository).deleteByIdAndUser(100L, testUser);
    }

    @Test
    @DisplayName("deleteTask() should return false when no task is deleted")
    void deleteTask_shouldReturnFalseWhenNotDeleted() {
        // ARRANGE: Mock the repository to return 0, indicating no rows were deleted.
        when(taskRepository.deleteByIdAndUser(999L, testUser)).thenReturn(0);

        // ACT
        boolean result = taskService.deleteTask(999L, testUser);

        // ASSERT
        assertFalse(result);
    }

    @Test
    @DisplayName("deleteTasks() should call repository with correct IDs and return deleted count")
    void deleteTasks_shouldCallRepositoryAndReturnCount() {
        // ARRANGE
        List<Long> taskIdsToDelete = List.of(100L, 101L, 102L);
        // Mock the repository to return 3, as if all 3 tasks were deleted.
        when(taskRepository.deleteByIdInAndUser(taskIdsToDelete, testUser)).thenReturn(3);

        // ACT
        int deletedCount = taskService.deleteTasks(taskIdsToDelete, testUser);

        // ASSERT
        assertEquals(3, deletedCount);
        // VERIFY that the batch delete method was called with the correct list of IDs.
        verify(taskRepository).deleteByIdInAndUser(taskIdsToDelete, testUser);
    }
}