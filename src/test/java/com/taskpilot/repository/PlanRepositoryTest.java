package com.taskpilot.repository;

import com.taskpilot.model.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class PlanRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlanRepository planRepository;

    // Tests for findByName()
    @Test
    @DisplayName("findByName() should return plan when name exists")
    void findByName_ShouldReturnPlan_WhenNameExists() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50, 3000000, List.of("price_123", "price_456"));
        entityManager.persistAndFlush(plan);

        // ACT
        Optional<Plan> result = planRepository.findByName("Pro");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Pro", result.get().getName());
        assertEquals(1000, result.get().getRequestsPerMonth());
        assertEquals(50, result.get().getRequestsPerDay());
    }

    @Test
    @DisplayName("findByName() should return empty when name does not exist")
    void findByName_ShouldReturnEmpty_WhenNameDoesNotExist() {
        // ACT
        Optional<Plan> result = planRepository.findByName("NonExistentPlan");

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByName() should handle case sensitivity correctly")
    void findByName_ShouldHandleCaseSensitivity() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50, 3000000, List.of("price_123"));
        entityManager.persistAndFlush(plan);

        // ACT
        Optional<Plan> result = planRepository.findByName("PRO");

        // ASSERT
        assertTrue(result.isEmpty()); // Assuming name is case-sensitive
    }

    @Test
    @DisplayName("findByName() should handle null name")
    void findByName_ShouldHandleNullName() {
        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Optional<Plan> result = planRepository.findByName(null);
            assertTrue(result.isEmpty());
        });
    }

    @Test
    @DisplayName("findByName() should handle empty string")
    void findByName_ShouldHandleEmptyString() {
        // ACT
        Optional<Plan> result = planRepository.findByName("");

        // ASSERT
        assertTrue(result.isEmpty());
    }

    // Tests for findByStripePriceId()
    @Test
    @DisplayName("findByStripePriceId() should return plan when stripe price id exists")
    void findByStripePriceId_ShouldReturnPlan_WhenStripePriceIdExists() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50, 3000000, List.of("price_monthly_123", "price_yearly_456"));
        entityManager.persistAndFlush(plan);

        // ACT
        Optional<Plan> result = planRepository.findByStripePriceId("price_monthly_123");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Pro", result.get().getName());
        assertTrue(result.get().getStripePriceIds().contains("price_monthly_123"));
    }

    @Test
    @DisplayName("findByStripePriceId() should return plan when stripe price id is second in list")
    void findByStripePriceId_ShouldReturnPlan_WhenStripePriceIdIsSecondInList() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50, 3000000, List.of("price_monthly_123", "price_yearly_456"));
        entityManager.persistAndFlush(plan);

        // ACT
        Optional<Plan> result = planRepository.findByStripePriceId("price_yearly_456");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Pro", result.get().getName());
        assertTrue(result.get().getStripePriceIds().contains("price_yearly_456"));
    }

    @Test
    @DisplayName("findByStripePriceId() should return empty when stripe price id does not exist")
    void findByStripePriceId_ShouldReturnEmpty_WhenStripePriceIdDoesNotExist() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50, 3000000, List.of("price_123", "price_456"));
        entityManager.persistAndFlush(plan);

        // ACT
        Optional<Plan> result = planRepository.findByStripePriceId("price_nonexistent");

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByStripePriceId() should handle null stripe price id")
    void findByStripePriceId_ShouldHandleNull_StripePriceId() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50, 3000000,List.of("price_123"));
        entityManager.persistAndFlush(plan);

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Optional<Plan> result = planRepository.findByStripePriceId(null);
            assertTrue(result.isEmpty());
        });
    }

    @Test
    @DisplayName("findByStripePriceId() should handle empty stripe price id")
    void findByStripePriceId_ShouldHandleEmpty_StripePriceId() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50,3000000, List.of("price_123"));
        entityManager.persistAndFlush(plan);

        // ACT
        Optional<Plan> result = planRepository.findByStripePriceId("");

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByStripePriceId() should work with multiple plans having different price ids")
    void findByStripePriceId_ShouldWorkWithMultiplePlans_WithDifferentPriceIds() {
        // ARRANGE
        Plan proPlan = new Plan("Pro", 1000, 50, 3000000,List.of("price_pro_monthly", "price_pro_yearly"));
        Plan basicPlan = new Plan("Basic", 100, 5,1000000, List.of("price_basic_monthly", "price_basic_yearly"));
        entityManager.persistAndFlush(proPlan);
        entityManager.persistAndFlush(basicPlan);

        // ACT
        Optional<Plan> proResult = planRepository.findByStripePriceId("price_pro_monthly");
        Optional<Plan> basicResult = planRepository.findByStripePriceId("price_basic_yearly");

        // ASSERT
        assertTrue(proResult.isPresent());
        assertEquals("Pro", proResult.get().getName());

        assertTrue(basicResult.isPresent());
        assertEquals("Basic", basicResult.get().getName());
    }

    // Tests for inherited JpaRepository methods
    @Test
    @DisplayName("save() should persist plan with all properties")
    void save_ShouldPersistPlan_WithAllProperties() {
        // ARRANGE
        Plan plan = new Plan("Enterprise", 5000, 200, 3000000,List.of("price_ent_monthly", "price_ent_yearly"));

        // ACT
        Plan savedPlan = planRepository.save(plan);

        // ASSERT
        assertNotNull(savedPlan.getId());
        assertEquals("Enterprise", savedPlan.getName());
        assertEquals(5000, savedPlan.getRequestsPerMonth());
        assertEquals(200, savedPlan.getRequestsPerDay());
        assertEquals(3000000, savedPlan.getMaxFileSize());
        assertEquals(2, savedPlan.getStripePriceIds().size());
        assertTrue(savedPlan.getStripePriceIds().contains("price_ent_monthly"));
        assertTrue(savedPlan.getStripePriceIds().contains("price_ent_yearly"));
    }

    @Test
    @DisplayName("save() should handle plan with empty stripe price ids list")
    void save_ShouldHandlePlan_WithEmptyStripePriceIdsList() {
        // ARRANGE
        Plan plan = new Plan("Free", 10, 1, 3000000,Collections.emptyList());

        // ACT
        Plan savedPlan = planRepository.save(plan);

        // ASSERT
        assertNotNull(savedPlan.getId());
        assertEquals("Free", savedPlan.getName());
        assertTrue(savedPlan.getStripePriceIds().isEmpty());
    }

    @Test
    @DisplayName("findById() should return plan when id exists")
    void findById_ShouldReturnPlan_WhenIdExists() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50,3000000, List.of("price_123"));
        Plan savedPlan = entityManager.persistAndFlush(plan);

        // ACT
        Optional<Plan> result = planRepository.findById(savedPlan.getId());

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Pro", result.get().getName());
    }

    @Test
    @DisplayName("findById() should return empty when id does not exist")
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        // ACT
        Optional<Plan> result = planRepository.findById(999L);

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("existsById() should return true when plan exists")
    void existsById_ShouldReturnTrue_WhenPlanExists() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50,3000000, List.of("price_123"));
        Plan savedPlan = entityManager.persistAndFlush(plan);

        // ACT
        boolean exists = planRepository.existsById(savedPlan.getId());

        // ASSERT
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsById() should return false when plan does not exist")
    void existsById_ShouldReturnFalse_WhenPlanDoesNotExist() {
        // ACT
        boolean exists = planRepository.existsById(999L);

        // ASSERT
        assertFalse(exists);
    }

    @Test
    @DisplayName("deleteById() should remove plan from database")
    void deleteById_ShouldRemovePlan_FromDatabase() {
        // ARRANGE
        Plan plan = new Plan("Pro", 1000, 50,3000000, List.of("price_123"));
        Plan savedPlan = entityManager.persistAndFlush(plan);
        Long planId = savedPlan.getId();

        // ACT
        planRepository.deleteById(planId);
        entityManager.flush();
        entityManager.clear();

        // ASSERT
        Optional<Plan> deletedPlan = planRepository.findById(planId);
        assertTrue(deletedPlan.isEmpty());
    }

    @Test
    @DisplayName("count() should return correct number of plans")
    void count_ShouldReturnCorrectNumber_OfPlans() {
        // ARRANGE
        Plan plan1 = new Plan("Basic", 100, 5,3000000, List.of("price_basic"));
        Plan plan2 = new Plan("Pro", 1000, 50, 3000000,List.of("price_pro"));
        entityManager.persistAndFlush(plan1);
        entityManager.persistAndFlush(plan2);

        // ACT
        long count = planRepository.count();

        // ASSERT
        assertEquals(2, count);
    }

    @Test
    @DisplayName("findAll() should return all plans")
    void findAll_ShouldReturnAllPlans() {
        // ARRANGE
        Plan plan1 = new Plan("Basic", 100, 5, 3000000, List.of("price_basic"));
        Plan plan2 = new Plan("Pro", 1000, 50, 3000000, List.of("price_pro"));
        entityManager.persistAndFlush(plan1);
        entityManager.persistAndFlush(plan2);

        // ACT
        List<Plan> result = planRepository.findAll();

        // ASSERT
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(plan -> "Basic".equals(plan.getName())));
        assertTrue(result.stream().anyMatch(plan -> "Pro".equals(plan.getName())));
    }
}