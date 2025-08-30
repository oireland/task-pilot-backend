package com.taskpilot.service;

import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private User testUser; // Make this a mock instead of creating a real instance

    @InjectMocks
    private UserService userService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 123L;
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String ENCRYPTED_TOKEN = "encrypted-access-token";
    private static final String WORKSPACE_ID = "workspace-123";
    private static final String WORKSPACE_NAME = "Test Workspace";
    private static final String WORKSPACE_ICON = "icon-url";
    private static final String BOT_ID = "bot-123";
    private static final String DATABASE_ID = "database-123";
    private static final String DATABASE_NAME = "Tasks Database";

    // --- Tests for the findUserByEmail method ---

    @Test
    @DisplayName("findUserByEmail() should return user when found")
    void findUserByEmail_shouldReturnUser_whenFound() {
        // ARRANGE
        when(testUser.getEmail()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // ACT
        Optional<User> result = userService.findUserByEmail(TEST_EMAIL);

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        assertEquals(TEST_EMAIL, result.get().getEmail());

        // VERIFY
        verify(userRepository).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("findUserByEmail() should return empty optional when not found")
    void findUserByEmail_shouldReturnEmpty_whenNotFound() {
        // ARRANGE
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // ACT
        Optional<User> result = userService.findUserByEmail("notfound@example.com");

        // ASSERT
        assertFalse(result.isPresent());

        // VERIFY
        verify(userRepository).findByEmail("notfound@example.com");
    }

    // --- Tests for the saveNotionIntegrationDetails method ---

    @Test
    @DisplayName("saveNotionIntegrationDetails() should encrypt token and save user details when user exists")
    void saveNotionIntegrationDetails_shouldEncryptAndSave_whenUserExists() {
        // ARRANGE
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt(ACCESS_TOKEN)).thenReturn(ENCRYPTED_TOKEN);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // ACT
        userService.saveNotionIntegrationDetails(
                TEST_USER_ID, ACCESS_TOKEN, WORKSPACE_ID,
                WORKSPACE_NAME, WORKSPACE_ICON, BOT_ID
        );

        // ASSERT & VERIFY
        verify(encryptionService).encrypt(ACCESS_TOKEN);
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(testUser);

        // Verify that all the setter methods were called on the user
        verify(testUser).setNotionAccessToken(ENCRYPTED_TOKEN);
        verify(testUser).setNotionWorkspaceId(WORKSPACE_ID);
        verify(testUser).setNotionWorkspaceName(WORKSPACE_NAME);
        verify(testUser).setNotionWorkspaceIcon(WORKSPACE_ICON);
        verify(testUser).setNotionBotId(BOT_ID);
    }

    @Test
    @DisplayName("saveNotionIntegrationDetails() should do nothing when user not found")
    void saveNotionIntegrationDetails_shouldDoNothing_whenUserNotFound() {
        // ARRANGE
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // ACT
        userService.saveNotionIntegrationDetails(
                TEST_USER_ID, ACCESS_TOKEN, WORKSPACE_ID,
                WORKSPACE_NAME, WORKSPACE_ICON, BOT_ID
        );

        // ASSERT & VERIFY
        verify(userRepository).findById(TEST_USER_ID);
        verify(encryptionService).encrypt(ACCESS_TOKEN); // Token still gets encrypted
        verify(userRepository, never()).save(any(User.class)); // But save is never called
    }

    @Test
    @DisplayName("saveNotionIntegrationDetails() should handle null parameters gracefully")
    void saveNotionIntegrationDetails_shouldHandleNullParameters() {
        // ARRANGE
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt(null)).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // ACT
        userService.saveNotionIntegrationDetails(
                TEST_USER_ID, null, null, null, null, null
        );

        // ASSERT & VERIFY
        verify(encryptionService).encrypt(null);
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(testUser);

        // Verify that setter methods were called with null values
        verify(testUser).setNotionAccessToken(null);
        verify(testUser).setNotionWorkspaceId(null);
        verify(testUser).setNotionWorkspaceName(null);
        verify(testUser).setNotionWorkspaceIcon(null);
        verify(testUser).setNotionBotId(null);
    }

    // --- Tests for the setNotionTargetDatabase method ---

    @Test
    @DisplayName("setNotionTargetDatabase() should update database details when user exists")
    void setNotionTargetDatabase_shouldUpdateDatabase_whenUserExists() {
        // ARRANGE
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // ACT
        userService.setNotionTargetDatabase(TEST_USER_ID, DATABASE_ID, DATABASE_NAME);

        // ASSERT & VERIFY
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(testUser);

        // Verify that the database setter methods were called
        verify(testUser).setNotionTargetDatabaseId(DATABASE_ID);
        verify(testUser).setNotionTargetDatabaseName(DATABASE_NAME);
    }

    @Test
    @DisplayName("setNotionTargetDatabase() should do nothing when user not found")
    void setNotionTargetDatabase_shouldDoNothing_whenUserNotFound() {
        // ARRANGE
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // ACT
        userService.setNotionTargetDatabase(TEST_USER_ID, DATABASE_ID, DATABASE_NAME);

        // ASSERT & VERIFY
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("setNotionTargetDatabase() should handle null parameters gracefully")
    void setNotionTargetDatabase_shouldHandleNullParameters() {
        // ARRANGE
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // ACT
        userService.setNotionTargetDatabase(TEST_USER_ID, null, null);

        // ASSERT & VERIFY
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(testUser);

        // Verify that setter methods were called with null values
        verify(testUser).setNotionTargetDatabaseId(null);
        verify(testUser).setNotionTargetDatabaseName(null);
    }

    // --- Edge case tests ---

    @Test
    @DisplayName("saveNotionIntegrationDetails() should handle encryption service exception")
    void saveNotionIntegrationDetails_shouldHandleEncryptionException() {
        // ARRANGE
        when(encryptionService.encrypt(ACCESS_TOKEN)).thenThrow(new RuntimeException("Encryption failed"));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.saveNotionIntegrationDetails(
                        TEST_USER_ID, ACCESS_TOKEN, WORKSPACE_ID,
                        WORKSPACE_NAME, WORKSPACE_ICON, BOT_ID
                )
        );

        assertEquals("Encryption failed", exception.getMessage());

        // VERIFY
        verify(encryptionService).encrypt(ACCESS_TOKEN);
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Methods should handle repository exceptions gracefully")
    void methods_shouldHandleRepositoryExceptions() {
        // ARRANGE
        when(userRepository.findById(TEST_USER_ID)).thenThrow(new RuntimeException("Database error"));

        // ACT & ASSERT for saveNotionIntegrationDetails
        when(encryptionService.encrypt(ACCESS_TOKEN)).thenReturn(ENCRYPTED_TOKEN);

        RuntimeException exception1 = assertThrows(RuntimeException.class, () ->
                userService.saveNotionIntegrationDetails(
                        TEST_USER_ID, ACCESS_TOKEN, WORKSPACE_ID,
                        WORKSPACE_NAME, WORKSPACE_ICON, BOT_ID
                )
        );
        assertEquals("Database error", exception1.getMessage());

        // ACT & ASSERT for setNotionTargetDatabase
        RuntimeException exception2 = assertThrows(RuntimeException.class, () ->
                userService.setNotionTargetDatabase(TEST_USER_ID, DATABASE_ID, DATABASE_NAME)
        );
        assertEquals("Database error", exception2.getMessage());

        // ACT & ASSERT for findUserByEmail
        when(userRepository.findByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception3 = assertThrows(RuntimeException.class, () ->
                userService.findUserByEmail(TEST_EMAIL)
        );
        assertEquals("Database error", exception3.getMessage());
    }
}
