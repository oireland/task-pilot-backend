package com.taskpilot.config;

import com.taskpilot.model.Plan;
import com.taskpilot.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final PlanRepository planRepository;

    // --- Injecting Free Plan Properties ---
    @Value("${plan.free.name}")
    private String freePlanName;
    @Value("${plan.free.requests-per-month}")
    private int freePlanRequestsPerMonth;
    @Value("${plan.free.requests-per-day}")
    private int freePlanRequestsPerDay;

    // --- Injecting Pro Plan Properties ---
    @Value("${plan.pro.name}")
    private String proPlanName;
    @Value("${plan.pro.price-ids}")
    private List<String> proPlanPriceIds;
    @Value("${plan.pro.requests-per-month}")
    private int proPlanRequestsPerMonth;
    @Value("${plan.pro.requests-per-day}")
    private int proPlanRequestsPerDay;


    public DatabaseSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Checking and seeding plan data...");

        // Seed the "Free" plan
        createPlanIfNotFound(
                freePlanName,
                freePlanRequestsPerMonth,
                freePlanRequestsPerDay,
                Collections.emptyList() // Free plan has no price IDs
        );

        // Seed the "Pro" plan
        createPlanIfNotFound(
                proPlanName,
                proPlanRequestsPerMonth,
                proPlanRequestsPerDay,
                proPlanPriceIds
        );

        logger.info("Plan data seeding complete.");
    }

    private void createPlanIfNotFound(String name, int reqMonth, int reqDay, List<String> priceIds) {
        // Check if a plan with this name already exists to avoid duplicates
        if (planRepository.findByName(name).isEmpty()) {
            Plan newPlan = new Plan(name, reqMonth, reqDay, priceIds);
            planRepository.save(newPlan);
            logger.info("Created new plan: '{}' with {} price IDs.", name, priceIds.size());
        } else {
            logger.info("Plan '{}' already exists. Skipping creation.", name);
        }
    }
}