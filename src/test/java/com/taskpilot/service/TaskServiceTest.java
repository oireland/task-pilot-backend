package com.taskpilot.service;

import com.taskpilot.dto.task.*;
import com.taskpilot.model.TaskList;
import com.taskpilot.model.Todo;
import com.taskpilot.model.User;
import com.taskpilot.repository.TaskListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskListRepository taskListRepository;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private TaskList testTaskList;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "password");
        testUser.setId(1L);

        testTaskList = createTaskList("Test Title", "Test Description", List.of("Item 1"), testUser);
        testTaskList.setId(100L);
        testTaskList.setCreatedAt(LocalDateTime.now().minusDays(1));
        testTaskList.setUpdatedAt(LocalDateTime.now());
    }

    private TaskList createTaskList(String title, String description, List<String> items, User user) {
        TaskList tl = new TaskList();
        tl.setTitle(title);
        tl.setDescription(description);
        tl.setUser(user);
        List<Todo> todos = new ArrayList<>();
        for (String c : items) {
            Todo t = new Todo();
            t.setContent(c);
            t.setChecked(false);
            t.setDeadline(null);
            t.setTaskList(tl);
            todos.add(t);
        }
        tl.setItems(todos);
        return tl;
    }

    // --- READ Operations ---

    @Test
    @DisplayName("getTaskByIdForUser() should return TaskDTO if found and owned by user")
    void getTaskByIdForUser_shouldReturnDtoWhenFound() {
        when(taskListRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testTaskList));

        Optional<TaskDTO> result = taskService.getTaskByIdForUser(100L, testUser);

        assertTrue(result.isPresent());
        assertEquals(testTaskList.getTitle(), result.get().title());
        assertEquals(testTaskList.getId(), result.get().id());
    }

    @Test
    @DisplayName("getTaskByIdForUser() should return empty Optional if not found")
    void getTaskByIdForUser_shouldReturnEmptyWhenNotFound() {
        when(taskListRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        Optional<TaskDTO> result = taskService.getTaskByIdForUser(999L, testUser);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTasksForUser() should return a page of tasks")
    void getTasksForUser_shouldReturnPageOfTasks() {
        Pageable pageable = Pageable.unpaged();
        Page<TaskList> taskPage = new PageImpl<>(List.of(testTaskList));
        when(taskListRepository.findAll(ArgumentMatchers.<Specification<TaskList>>any(), eq(pageable))).thenReturn(taskPage);

        Page<TaskDTO> resultPage = taskService.getTasksForUser(testUser, null, pageable);

        assertEquals(1, resultPage.getTotalElements());
        assertEquals("Test Title", resultPage.getContent().getFirst().title());
    }

    // --- CREATE Operations ---

    @Test
    @DisplayName("createTask(ExtractedTaskListDTO) should save a new task")
    void createTaskFromExtractedDTO_shouldSaveNewTask() {
        ExtractedTaskListDTO dto = new ExtractedTaskListDTO("Extracted Title", "Extracted Desc", List.of("Task A"));
        when(taskListRepository.save(any(TaskList.class))).thenReturn(new TaskList());

        taskService.createTask(dto, testUser);

        ArgumentCaptor<TaskList> taskCaptor = ArgumentCaptor.forClass(TaskList.class);
        verify(taskListRepository).save(taskCaptor.capture());

        TaskList savedTaskList = taskCaptor.getValue();
        assertEquals("Extracted Title", savedTaskList.getTitle());
        assertEquals(testUser, savedTaskList.getUser());
        assertEquals(1, savedTaskList.getItems().size());
        assertEquals("Task A", savedTaskList.getItems().getFirst().getContent());
    }

    @Test
    @DisplayName("createTask(CreateTaskDTO) should save and return a TaskDTO")
    void createTaskFromCreateDTO_shouldSaveAndReturnDTO() {
        CreateTaskDTO dto = new CreateTaskDTO(
                "Manual Title",
                "Manual Desc",
                List.of(new TodoDTO(null, "Manual Item", false, null))
        );
        when(taskListRepository.save(any(TaskList.class))).thenReturn(testTaskList);

        TaskDTO resultDTO = taskService.createTask(dto, testUser);

        assertNotNull(resultDTO);
        assertEquals(testTaskList.getId(), resultDTO.id());
        assertEquals("Test Title", resultDTO.title());
    }

    // --- UPDATE Operation ---

    @Test
    @DisplayName("updateTask() should update and save the task if found")
    void updateTask_shouldUpdateIfFound() {
        UpdateTaskDTO updateDto = new UpdateTaskDTO(
                "Updated Title",
                "Updated Desc",
                List.of(new TodoDTO(null, "Updated Item", false, null))
        );
        when(taskListRepository.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testTaskList));
        when(taskListRepository.save(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TaskDTO> result = taskService.updateTask(100L, updateDto, testUser);

        assertTrue(result.isPresent());
        assertEquals("Updated Title", result.get().title());
        assertEquals("Updated Desc", result.get().description());
        verify(taskListRepository).save(any(TaskList.class));
    }

    @Test
    @DisplayName("updateTask() should return empty Optional if task is not found")
    void updateTask_shouldReturnEmptyIfNotFound() {
        UpdateTaskDTO updateDto = new UpdateTaskDTO("Title", "Desc", List.of());
        when(taskListRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        Optional<TaskDTO> result = taskService.updateTask(999L, updateDto, testUser);

        assertTrue(result.isEmpty());
        verify(taskListRepository, never()).save(any(TaskList.class));
    }

    // --- DELETE Operations ---

    @Test
    @DisplayName("deleteTask() should return true when a task is deleted")
    void deleteTask_shouldReturnTrueWhenDeleted() {
        when(taskListRepository.deleteByIdAndUser(100L, testUser)).thenReturn(1);

        boolean result = taskService.deleteTask(100L, testUser);

        assertTrue(result);
        verify(taskListRepository).deleteByIdAndUser(100L, testUser);
    }

    @Test
    @DisplayName("deleteTask() should return false when no task is deleted")
    void deleteTask_shouldReturnFalseWhenNotDeleted() {
        when(taskListRepository.deleteByIdAndUser(999L, testUser)).thenReturn(0);

        boolean result = taskService.deleteTask(999L, testUser);

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteTasks() should call repository with correct IDs and return deleted count")
    void deleteTasks_shouldCallRepositoryAndReturnCount() {
        List<Long> taskIdsToDelete = List.of(100L, 101L, 102L);
        when(taskListRepository.deleteByIdInAndUser(taskIdsToDelete, testUser)).thenReturn(3);

        int deletedCount = taskService.deleteTasks(taskIdsToDelete, testUser);

        assertEquals(3, deletedCount);
        verify(taskListRepository).deleteByIdInAndUser(taskIdsToDelete, testUser);
    }
}