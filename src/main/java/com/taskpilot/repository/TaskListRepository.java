package com.taskpilot.repository;

import com.taskpilot.model.TaskList;
import com.taskpilot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskListRepository extends JpaRepository<TaskList, Long>, JpaSpecificationExecutor<TaskList> {

    Optional<TaskList> findByIdAndUser(Long taskId, User user);

    @Modifying
    @Query("DELETE FROM TaskList t WHERE t.id = :taskId AND t.user = :user")
    int deleteByIdAndUser(@Param("taskId") Long taskId, @Param("user") User user);

    @Modifying
    @Query("DELETE FROM TaskList t WHERE t.id IN :taskIds AND t.user = :user")
    int deleteByIdInAndUser(@Param("taskIds") List<Long> taskIds, @Param("user") User user);
}