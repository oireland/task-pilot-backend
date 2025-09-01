package com.taskpilot.service;

import com.taskpilot.dto.task.*;
import com.taskpilot.model.TaskList;
import com.taskpilot.model.Todo;
import com.taskpilot.model.User;
import com.taskpilot.repository.TaskListRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskListRepository taskListRepository;

    public TaskService(TaskListRepository taskListRepository) {
        this.taskListRepository = taskListRepository;
    }

    @Transactional(readOnly = true)
    public Page<TaskListDTO> getTasksForUser(User user, String searchTerm, Pageable pageable) {
        Specification<TaskList> spec = (root, _, cb) -> cb.equal(root.get("user"), user);

        if (StringUtils.hasText(searchTerm)) {
            Specification<TaskList> searchSpec = (root, _, cb) -> {
                String pattern = "%" + searchTerm.toLowerCase() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                );
            };
            spec = spec.and(searchSpec);
        }

        Page<TaskList> tasksPage = taskListRepository.findAll(spec, pageable);
        return tasksPage.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<TaskListDTO> getTaskListByIdForUser(Long taskId, User user) {
        return taskListRepository.findByIdAndUser(taskId, user).map(this::convertToDto);
    }

    @Transactional
    public TaskListDTO createTaskList(ExtractedTaskListDTO docData, User user) {
        TaskList newTaskList = new TaskList();
        newTaskList.setTitle(docData.title());
        newTaskList.setDescription(docData.description());
        newTaskList.setUser(user);
        newTaskList.setTodos(new ArrayList<>());

        List<Todo> todos = toTodosFromStrings(docData.todos(), newTaskList);
        newTaskList.getTodos().addAll(todos);

        TaskList saved = taskListRepository.save(newTaskList);
        return convertToDto(saved);
    }

    @Transactional
    public TaskListDTO createTaskList(CreateTaskDTO taskData, User user) {
        TaskList newTaskList = new TaskList();
        newTaskList.setTitle(taskData.title());
        newTaskList.setDescription(taskData.description());
        newTaskList.setUser(user);
        newTaskList.setTodos(new ArrayList<>());

        List<Todo> todos = toTodosFromDtos(taskData.todos(), newTaskList);
        newTaskList.getTodos().addAll(todos);

        TaskList saved = taskListRepository.save(newTaskList);
        return convertToDto(saved);
    }

    @Transactional
    public Optional<TaskListDTO> updateTask(Long taskId, UpdateTaskDTO taskData, User user) {
        return taskListRepository.findByIdAndUser(taskId, user)
                .map(existing -> {
                    existing.setTitle(taskData.title());
                    existing.setDescription(taskData.description());

                    List<Todo> newTodos = toTodosFromDtos(taskData.todos(), existing);
                    existing.getTodos().clear();
                    existing.getTodos().addAll(newTodos);

                    TaskList updated = taskListRepository.save(existing);
                    return convertToDto(updated);
                });
    }

    @Transactional
    public boolean deleteTask(Long taskId, User user) {
        int deletedRows = taskListRepository.deleteByIdAndUser(taskId, user);
        return deletedRows > 0;
    }

    @Transactional
    public int deleteTasks(List<Long> taskIds, User user) {
        if (taskIds == null || taskIds.isEmpty()) {
            return 0;
        }
        return taskListRepository.deleteByIdInAndUser(taskIds, user);
    }

    @Transactional
    public int batchUpdateTasks(List<UpdateTaskWithIdDTO> tasksToUpdate, User user) {
        if (tasksToUpdate == null || tasksToUpdate.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (UpdateTaskWithIdDTO taskData : tasksToUpdate) {
            Optional<TaskListDTO> updated = updateTask(
                    taskData.id(),
                    new UpdateTaskDTO(taskData.title(), taskData.description(), taskData.todos()),
                    user
            );
            if (updated.isPresent()) {
                updatedCount++;
            }
        }

        return updatedCount;
    }

    @Transactional
    public boolean updateTodoCheckedStatus(Long todoId, boolean checked, User user) {
        return taskListRepository.updateTodoCheckedStatus(todoId, checked, user.getId()) > 0;
    }

    private List<Todo> toTodosFromStrings(List<String> items, TaskList parent) {
        List<Todo> todos = new ArrayList<>();
        if (items == null) return todos;

        for (String content : items) {
            if (!StringUtils.hasText(content)) continue;
            Todo t = new Todo();
            t.setContent(content.trim());
            t.setChecked(false);
            t.setDeadline(null);
            t.setTaskList(parent);
            todos.add(t);
        }
        return todos;
    }

    private List<Todo> toTodosFromDtos(List<TodoDTO> items, TaskList parent) {
        List<Todo> todos = new ArrayList<>();
        if (items == null) return todos;

        for (TodoDTO dto : items) {
            if (dto == null || !StringUtils.hasText(dto.content())) continue;
            Todo t = new Todo();
            // Ignore incoming id; replace semantics with orphanRemoval=true
            t.setContent(dto.content().trim());
            t.setChecked(dto.checked());
            t.setDeadline(dto.deadline());
            t.setTaskList(parent);
            todos.add(t);
        }
        return todos;
    }

    private TaskListDTO convertToDto(TaskList task) {
        List<TodoDTO> todoDTOs = new ArrayList<>();
        if (task.getTodos() != null) {
            for (Todo t : task.getTodos()) {
                todoDTOs.add(new TodoDTO(
                        t.getId(),
                        t.getContent(),
                        t.isChecked(),
                        t.getDeadline()
                ));
            }
        }
        return new TaskListDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                todoDTOs,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}