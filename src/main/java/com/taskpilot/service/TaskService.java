package com.taskpilot.service;

import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.dto.task.TaskDTO;
import com.taskpilot.model.Task;
import com.taskpilot.model.User;
import com.taskpilot.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Retrieves a paginated list of tasks for a specific user, with optional search.
     * @param user The user whose tasks are to be retrieved.
     * @param searchTerm An optional term to search for in task titles and descriptions.
     * @param pageable Pagination and sorting information.
     * @return A page of TaskDTOs.
     */
    @Transactional(readOnly = true)
    public Page<TaskDTO> getTasksForUser(User user, String searchTerm, Pageable pageable) {
        // Base specification: always filter by the current user.
        Specification<Task> spec = (root, query, cb) -> cb.equal(root.get("user"), user);

        // If a search term is provided, add a 'like' clause for title and description.
        if (StringUtils.hasText(searchTerm)) {
            Specification<Task> searchSpec = (root, query, cb) -> {
                String pattern = "%" + searchTerm.toLowerCase() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                );
            };
            spec = spec.and(searchSpec);
        }

        // Fetch the page of Task entities
        Page<Task> tasksPage = taskRepository.findAll(spec, pageable);

        // Map the Page<Task> to Page<TaskDTO> within the service
        return tasksPage.map(this::convertToDto);
    }

    /**
     * Creates and saves a new task list from extracted document data.
     * @param docData The data extracted from the document.
     * @param user The user who owns the task.
     * @return The newly saved Task entity.
     */
    @Transactional
    public Task createTask(ExtractedDocDataDTO docData, User user) {
        Task newTask = new Task();
        newTask.setTitle(docData.title());
        newTask.setDescription(docData.description());
        newTask.setItems(docData.tasks());
        newTask.setUser(user);
        return taskRepository.save(newTask);
    }

    /**
     * Deletes a task by its ID, ensuring it belongs to the specified user.
     * This operation is performed in a single, atomic database query.
     *
     * @param taskId The ID of the task to delete.
     * @param user The user requesting the deletion.
     * @return {@code true} if a task was deleted, {@code false} otherwise.
     */
    @Transactional
    public boolean deleteTask(Long taskId, User user) {
        // This single call attempts to delete the task only if both the ID and user match.
        // It returns the number of rows affected by the delete operation.
        int deletedRows = taskRepository.deleteByIdAndUser(taskId, user);

        // If deletedRows is 1, the task was found and deleted.
        // If it's 0, no matching task was found (either wrong ID or wrong user),
        // and no delete operation occurred.
        return deletedRows > 0;
    }

    /**
     * Helper method to convert a Task entity to a TaskDTO.
     * @param task The Task entity.
     * @return The corresponding TaskDTO.
     */
    private TaskDTO convertToDto(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getItems(),
                task.getCreatedAt()
        );
    }
}