package com.taskpilot.repository;

import com.taskpilot.model.User;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class UserRepositoryTest {

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
    private UserRepository userRepository;

    // Tests for findByEmail()
    @Test
    @DisplayName("findByEmail() should return user when email exists")
    void findByEmail_ShouldReturnUser_WhenEmailExists() {
        // ARRANGE
        User user = new User();
        user.setEmail("john@example.com");
        user.setPassword("hashedPassword");
        entityManager.persistAndFlush(user);

        // ACT
        Optional<User> result = userRepository.findByEmail("john@example.com");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("findByEmail() should return empty when email does not exist")
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        // ACT
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByEmail() should handle case sensitivity correctly")
    void findByEmail_ShouldHandleCaseSensitivity() {
        // ARRANGE
        User user = new User();
        user.setEmail("john@example.com");
        user.setPassword("hashedPassword");
        entityManager.persistAndFlush(user);

        // ACT
        Optional<User> result = userRepository.findByEmail("JOHN@EXAMPLE.COM");

        // ASSERT
        assertTrue(result.isEmpty()); // Assuming email is case-sensitive
    }

    @Test
    @DisplayName("findByEmail() should handle null email")
    void findByEmail_ShouldHandleNullEmail() {
        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Optional<User> result = userRepository.findByEmail(null);
            assertTrue(result.isEmpty());
        });
    }

    // Tests for findByPlanRefreshDateBefore()
    @Test
    @DisplayName("findByPlanRefreshDateBefore() should return users with refresh date before specified date")
    void findByPlanRefreshDateBefore_ShouldReturnUsers_WhenRefreshDateIsBefore() {
        // ARRANGE
        LocalDate targetDate = LocalDate.now();

        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setPlanRefreshDate(targetDate.minusDays(1)); // Before target date
        entityManager.persistAndFlush(user1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setPlanRefreshDate(targetDate.minusDays(5)); // Before target date
        entityManager.persistAndFlush(user2);

        User user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setPassword("password");
        user3.setPlanRefreshDate(targetDate.plusDays(1)); // After target date
        entityManager.persistAndFlush(user3);

        // ACT
        List<User> result = userRepository.findByPlanRefreshDateBefore(targetDate);

        // ASSERT
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(user -> "user1@example.com".equals(user.getEmail())));
        assertTrue(result.stream().anyMatch(user -> "user2@example.com".equals(user.getEmail())));
        assertTrue(result.stream().noneMatch(user -> "user3@example.com".equals(user.getEmail())));
    }

    @Test
    @DisplayName("findByPlanRefreshDateBefore() should return empty list when no users have refresh date before specified date")
    void findByPlanRefreshDateBefore_ShouldReturnEmpty_WhenNoUsersHaveRefreshDateBefore() {
        // ARRANGE
        LocalDate targetDate = LocalDate.now();

        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("password");
        user.setPlanRefreshDate(targetDate.plusDays(1)); // After target date
        entityManager.persistAndFlush(user);

        // ACT
        List<User> result = userRepository.findByPlanRefreshDateBefore(targetDate);

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByPlanRefreshDateBefore() should handle users with null planRefreshDate")
    void findByPlanRefreshDateBefore_ShouldHandleNull_PlanRefreshDate() {
        // ARRANGE
        LocalDate targetDate = LocalDate.now();

        User userWithNullDate = new User();
        userWithNullDate.setEmail("null-date@example.com");
        userWithNullDate.setPassword("password");
        userWithNullDate.setPlanRefreshDate(null);
        entityManager.persistAndFlush(userWithNullDate);

        User userWithValidDate = new User();
        userWithValidDate.setEmail("valid-date@example.com");
        userWithValidDate.setPassword("password");
        userWithValidDate.setPlanRefreshDate(targetDate.minusDays(1));
        entityManager.persistAndFlush(userWithValidDate);

        // ACT
        List<User> result = userRepository.findByPlanRefreshDateBefore(targetDate);

        // ASSERT
        assertEquals(1, result.size());
        assertEquals("valid-date@example.com", result.get(0).getEmail());
    }

    @Test
    @DisplayName("findByPlanRefreshDateBefore() should return empty list when no users exist")
    void findByPlanRefreshDateBefore_ShouldReturnEmpty_WhenNoUsersExist() {
        // ACT
        List<User> result = userRepository.findByPlanRefreshDateBefore(LocalDate.now());

        // ASSERT
        assertTrue(result.isEmpty());
    }

    // Tests for resetAllDailyCounts()
    @Test
    @DisplayName("resetAllDailyCounts() should reset requestsInCurrentDay to 0 for all users")
    void resetAllDailyCounts_ShouldResetRequestsInCurrentDay_ForAllUsers() {
        // ARRANGE
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setRequestsInCurrentDay(5);
        entityManager.persistAndFlush(user1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setRequestsInCurrentDay(10);
        entityManager.persistAndFlush(user2);

        User user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setPassword("password");
        user3.setRequestsInCurrentDay(0); // Already 0
        entityManager.persistAndFlush(user3);

        // ACT
        int updatedCount = userRepository.resetAllDailyCounts();
        entityManager.flush();
        entityManager.clear();

        // ASSERT
        assertEquals(3, updatedCount); // Should update all 3 users

        // Verify all users have requestsInCurrentDay reset to 0
        User updatedUser1 = entityManager.find(User.class, user1.getId());
        User updatedUser2 = entityManager.find(User.class, user2.getId());
        User updatedUser3 = entityManager.find(User.class, user3.getId());

        assertEquals(0, updatedUser1.getRequestsInCurrentDay());
        assertEquals(0, updatedUser2.getRequestsInCurrentDay());
        assertEquals(0, updatedUser3.getRequestsInCurrentDay());
    }

    @Test
    @DisplayName("resetAllDailyCounts() should return 0 when no users exist")
    void resetAllDailyCounts_ShouldReturn0_WhenNoUsersExist() {
        // ACT
        int updatedCount = userRepository.resetAllDailyCounts();

        // ASSERT
        assertEquals(0, updatedCount);
    }

    // Tests for inherited CrudRepository methods
    @Test
    @DisplayName("save() should persist user with all properties")
    void save_ShouldPersistUser_WithAllProperties() {
        // ARRANGE
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setRequestsInCurrentDay(3);
        user.setPlanRefreshDate(LocalDate.now());

        // ACT
        User savedUser = userRepository.save(user);

        // ASSERT
        assertNotNull(savedUser.getId());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("hashedPassword", savedUser.getPassword());
        assertEquals(3, savedUser.getRequestsInCurrentDay());
        assertEquals(LocalDate.now(), savedUser.getPlanRefreshDate());
    }

    @Test
    @DisplayName("findById() should return user when id exists")
    void findById_ShouldReturnUser_WhenIdExists() {
        // ARRANGE
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        User savedUser = entityManager.persistAndFlush(user);

        // ACT
        Optional<User> result = userRepository.findById(savedUser.getId());

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("findById() should return empty when id does not exist")
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        // ACT
        Optional<User> result = userRepository.findById(999L);

        // ASSERT
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("existsById() should return true when user exists")
    void existsById_ShouldReturnTrue_WhenUserExists() {
        // ARRANGE
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        User savedUser = entityManager.persistAndFlush(user);

        // ACT
        boolean exists = userRepository.existsById(savedUser.getId());

        // ASSERT
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsById() should return false when user does not exist")
    void existsById_ShouldReturnFalse_WhenUserDoesNotExist() {
        // ACT
        boolean exists = userRepository.existsById(999L);

        // ASSERT
        assertFalse(exists);
    }

    @Test
    @DisplayName("deleteById() should remove user from database")
    void deleteById_ShouldRemoveUser_FromDatabase() {
        // ARRANGE
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        User savedUser = entityManager.persistAndFlush(user);
        Long userId = savedUser.getId();

        // ACT
        userRepository.deleteById(userId);
        entityManager.flush();
        entityManager.clear();

        // ASSERT
        Optional<User> deletedUser = userRepository.findById(userId);
        assertTrue(deletedUser.isEmpty());
    }

    @Test
    @DisplayName("count() should return correct number of users")
    void count_ShouldReturnCorrectNumber_OfUsers() {
        // ARRANGE
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        entityManager.persistAndFlush(user1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        entityManager.persistAndFlush(user2);

        // ACT
        long count = userRepository.count();

        // ASSERT
        assertEquals(2, count);
    }

    @Test
    @DisplayName("findAll() should return all users")
    void findAll_ShouldReturnAllUsers() {
        // ARRANGE
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        entityManager.persistAndFlush(user1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        entityManager.persistAndFlush(user2);

        // ACT
        Iterable<User> result = userRepository.findAll();
        List<User> users = (List<User>) result;

        // ASSERT
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(user -> "user1@example.com".equals(user.getEmail())));
        assertTrue(users.stream().anyMatch(user -> "user2@example.com".equals(user.getEmail())));
    }
}