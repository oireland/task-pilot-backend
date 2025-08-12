package com.taskpilot.config;

import com.taskpilot.model.Plan;
import com.taskpilot.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;

    public DatabaseSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) {
        // Create FREE plan if it doesn't exist
        if (planRepository.findByName("FREE").isEmpty()) {
            planRepository.save(new Plan("FREE", 30, 5)); // 30 requests/month
        }

        // Create PRO plan if it doesn't exist
        if (planRepository.findByName("PRO").isEmpty()) {
            planRepository.save(new Plan("PRO", 1000, 50)); // 1000 requests/month
        }
    }
}