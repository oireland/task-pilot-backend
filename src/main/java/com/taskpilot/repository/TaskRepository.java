package com.taskpilot.repository;

import com.taskpilot.model.Task;
import com.taskpilot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * Finds a task by its ID and owning user.
     * This is crucial for ensuring a user can only edit their own tasks.
     *
     * @param taskId The ID of the task to find.
     * @param user The user who must own the task.
     * @return An Optional containing the task if found and owned by the user.
     */
    Optional<Task> findByIdAndUser(Long taskId, User user);

    /**
     * Deletes a task by its ID and owning user in a single query.
     * The @Modifying annotation is required for queries that change data.
     *
     * @param taskId The ID of the task to delete.
     * @param user The user who must own the task.
     * @return The number of rows deleted (will be 0 or 1).
     */
    @Modifying
    @Query("DELETE FROM Task t WHERE t.id = :taskId AND t.user = :user")
    int deleteByIdAndUser(@Param("taskId") Long taskId, @Param("user") User user);

    /**
     * Deletes multiple tasks by a list of IDs for a specific user.
     *
     * @param taskIds The list of task IDs to delete.
     * @param user The user who must own the tasks.
     * @return The number of rows deleted.
     */
    @Modifying
    @Query("DELETE FROM Task t WHERE t.id IN :taskIds AND t.user = :user")
    int deleteByIdInAndUser(@Param("taskIds") List<Long> taskIds, @Param("user") User user);

}