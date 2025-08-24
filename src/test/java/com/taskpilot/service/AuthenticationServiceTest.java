package com.taskpilot.service;

import com.taskpilot.dto.auth.LoginUserDTO;
import com.taskpilot.dto.auth.RegisterUserDTO;
import com.taskpilot.dto.auth.VerifyUserDTO;
import com.taskpilot.model.Plan;
import com.taskpilot.model.User;
import com.taskpilot.repository.PlanRepository;
import com.taskpilot.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    // --- Mocks for all dependencies ---
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;
    @Mock
    private PlanRepository planRepository;

    // --- The service we are testing ---
    @InjectMocks
    private AuthenticationService authenticationService;

    // --- Reusable test data ---
    private RegisterUserDTO registerUserDTO;
    private Plan freePlan;
    private User testUser;

    @BeforeEach
    void setUp() {
        // This method runs before each test to set up common objects.
        registerUserDTO = new RegisterUserDTO("test@example.com", "Password123!");
        freePlan = new Plan("Free", 15, 3, null);
        testUser = new User("test@example.com", "encodedPassword");
        testUser.setPlan(freePlan);
        // Mock value from application.properties
        ReflectionTestUtils.setField(authenticationService, "FREE_PLAN_NAME", "Free");

    }

    // --- Tests for the signup method ---

    @Test
    @DisplayName("signup() should create and save a new user with a free plan")
    void signup_shouldCreateAndSaveNewUser() throws MessagingException {
        // ARRANGE: Configure mocks for a successful signup.
        when(planRepository.findByName("Free")).thenReturn(Optional.of(freePlan));
        when(passwordEncoder.encode(registerUserDTO.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the user that was passed to save()

        // ACT: Call the signup method.
        User createdUser = authenticationService.signup(registerUserDTO);

        // ASSERT: Verify the results.
        assertNotNull(createdUser);
        assertEquals(registerUserDTO.email(), createdUser.getEmail());
        assertFalse(createdUser.isEnabled()); // User should be disabled until verified.
        assertNotNull(createdUser.getVerificationCode()); // A verification code should be set.
        assertEquals("Free", createdUser.getPlan().getName()); // The free plan should be assigned.

        // VERIFY: Ensure that our mocked methods were called correctly.
        verify(passwordEncoder).encode("Password123!");
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString(), anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("signup() should throw exception if the default free plan is not found")
    void signup_shouldThrowExceptionWhenFreePlanNotFound() {
        // ARRANGE: Mock the plan repository to find no "Free" plan.
        when(planRepository.findByName("Free")).thenReturn(Optional.empty());

        // ACT & ASSERT: Expect a RuntimeException when signup is called.
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.signup(registerUserDTO));

        assertEquals("Default Free plan not found.", exception.getMessage());
    }


    // --- Tests for the authenticate method ---

    @Test
    @DisplayName("authenticate() should return user for valid credentials and enabled account")
    void authenticate_shouldReturnUserForValidCredentials() {
        // ARRANGE
        LoginUserDTO loginUserDTO = new LoginUserDTO("test@example.com", "Password123!");
        testUser.setEnabled(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // ACT
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);

        // ASSERT
        assertNotNull(authenticatedUser);
        assertEquals(testUser.getEmail(), authenticatedUser.getEmail());
        // VERIFY that the authentication manager was called.
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(loginUserDTO.email(), loginUserDTO.password())
        );
    }

    @Test
    @DisplayName("authenticate() should throw exception if user is not found")
    void authenticate_shouldThrowExceptionIfUserNotFound() {
        // ARRANGE
        LoginUserDTO loginUserDTO = new LoginUserDTO("notfound@example.com", "password");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.authenticate(loginUserDTO));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("authenticate() should throw exception if user is not enabled")
    void authenticate_shouldThrowExceptionIfNotEnabled() {
        // ARRANGE
        LoginUserDTO loginUserDTO = new LoginUserDTO("test@example.com", "Password123!");
        testUser.setEnabled(false); // The user is not yet verified.
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.authenticate(loginUserDTO));
        assertEquals("User account is not verified", exception.getMessage());
    }

    // --- Tests for the verifyUser method ---

    @Test
    @DisplayName("verifyUser() should enable user for valid, unexpired code")
    void verifyUser_shouldEnableUserForValidCode() {
        // ARRANGE
        String validCode = "123456";
        VerifyUserDTO verifyUserDTO = new VerifyUserDTO("test@example.com", validCode);
        testUser.setVerificationCode(validCode);
        testUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10)); // Not expired
        testUser.setEnabled(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // ACT
        User verifiedUser = authenticationService.verifyUser(verifyUserDTO);

        // ASSERT
        assertNotNull(verifiedUser);
        assertTrue(verifiedUser.isEnabled());
        assertNull(verifiedUser.getVerificationCode()); // Code should be cleared after verification.
        assertNull(verifiedUser.getVerificationCodeExpiresAt());

        // VERIFY: We use an ArgumentCaptor to inspect the user object that was saved.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isEnabled());
    }

    @Test
    @DisplayName("verifyUser() should throw exception for an invalid code")
    void verifyUser_shouldThrowExceptionForInvalidCode() {
        // ARRANGE
        VerifyUserDTO verifyUserDTO = new VerifyUserDTO("test@example.com", "wrong-code");
        testUser.setVerificationCode("correct-code");
        testUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.verifyUser(verifyUserDTO));
        assertEquals("Invalid verification code", exception.getMessage());
    }

    @Test
    @DisplayName("verifyUser() should throw exception for an expired code")
    void verifyUser_shouldThrowExceptionForExpiredCode() {
        // ARRANGE
        String expiredCode = "123456";
        VerifyUserDTO verifyUserDTO = new VerifyUserDTO("test@example.com", expiredCode);
        testUser.setVerificationCode(expiredCode);
        testUser.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(10)); // Expired!
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.verifyUser(verifyUserDTO));
        assertEquals("Verification code has expired", exception.getMessage());
    }

    // --- Tests for resendVerificationCode method ---
    @Test
    @DisplayName("resendVerificationCode() should send new code for unverified user")
    void resendVerificationCode_shouldWorkForUnverifiedUser() throws MessagingException {
        // ARRANGE
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // ACT
        authenticationService.resendVerificationCode("test@example.com");

        // VERIFY
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("resendVerificationCode() should throw exception if user is already verified")
    void resendVerificationCode_shouldThrowExceptionIfAlreadyVerified() {
        // ARRANGE
        testUser.setEnabled(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.resendVerificationCode("test@example.com"));
        assertEquals("User account is already verified", exception.getMessage());
    }
}