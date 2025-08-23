package com.taskpilot.repository;

import com.taskpilot.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Finds a plan by its unique name. Used for data seeding.
     */
    Optional<Plan> findByName(String name);

    /**
     * Finds a plan by checking if the given Stripe Price ID exists in its
     * list of associated price IDs. This is essential for the webhook handler.
     *
     * @param stripePriceId The Stripe Price ID from a webhook event.
     * @return The matching Plan, if any.
     */
    @Query("SELECT p FROM Plan p JOIN p.stripePriceIds spid WHERE spid = :stripePriceId")
    Optional<Plan> findByStripePriceId(@Param("stripePriceId") String stripePriceId);
}