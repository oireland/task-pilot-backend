package com.taskpilot.repository;

import com.taskpilot.model.Plan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends CrudRepository<Plan, Long> {
    Optional<Plan> findByName(String name);
}