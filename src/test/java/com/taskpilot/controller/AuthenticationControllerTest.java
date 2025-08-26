// Java
package com.taskpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.dto.auth.LoginUserDTO;
import com.taskpilot.dto.auth.RegisterUserDTO;
import com.taskpilot.dto.auth.VerifyUserDTO;
import com.taskpilot.model.User;
import com.taskpilot.service.AuthenticationService;
import com.taskpilot.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    static class ValidationConfig {
        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @Nested
    class SignupTests {

        @Test
        @DisplayName("POST /api/v1/auth/signup returns 200 with success message for valid data")
        void signup_returnsOk_whenDataIsValid() throws Exception {
            RegisterUserDTO valid = new RegisterUserDTO("test@example.com", "Password123!");
            User created = new User("test@example.com", "encodedPassword");
            when(authenticationService.signup(any(RegisterUserDTO.class))).thenReturn(created);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(valid)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User registered successfully. Please check your email."));

            verify(authenticationService).signup(ArgumentMatchers.any(RegisterUserDTO.class));
        }

        @Test
        @DisplayName("POST /api/v1/auth/signup returns 400 for invalid payload")
        void signup_returnsBadRequest_whenDataIsInvalid() throws Exception {
            // invalid email and too short password, relies on DTO \@Valid constraints
            String invalidJson = "{\"email\":\"not-an-email\",\"password\":\"123\"}";

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class LoginTests {

        @Test
        @DisplayName("POST /api/v1/auth/login returns 200 with token for valid credentials")
        void login_returnsOk_withToken_whenValid() throws Exception {
            LoginUserDTO req = new LoginUserDTO("test@example.com", "Password123!");
            User authenticated = new User("test@example.com", "encodedPassword");
            String token = "dummy.jwt.token";

            when(authenticationService.authenticate(any(LoginUserDTO.class))).thenReturn(authenticated);
            when(jwtService.generateToken(authenticated)).thenReturn(token);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(token));
        }

        @Test
        @DisplayName("POST /api/v1/auth/login returns 400 for invalid payload")
        void login_returnsBadRequest_whenInvalidPayload() throws Exception {
            String invalidJson = "{\"email\":\"bad-email\",\"password\":\"123\"}";

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/v1/auth/login returns 500 when service throws")
        void login_returnsServerError_whenServiceThrows() throws Exception {
            LoginUserDTO req = new LoginUserDTO("test@example.com", "Password123!");
            when(authenticationService.authenticate(any(LoginUserDTO.class)))
                    .thenThrow(new RuntimeException("User not found"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    class VerifyTests {

        @Test
        @DisplayName("POST /api/v1/auth/verify returns 200 with token for valid code")
        void verify_returnsOk_withToken_whenValid() throws Exception {
            VerifyUserDTO req = new VerifyUserDTO("test@example.com", "123456");
            User verified = new User("test@example.com", "encodedPassword");
            String token = "dummy.jwt.token.after.verify";

            when(authenticationService.verifyUser(any(VerifyUserDTO.class))).thenReturn(verified);
            when(jwtService.generateToken(verified)).thenReturn(token);

            mockMvc.perform(post("/api/v1/auth/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(token));
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify returns 400 with message when verification fails")
        void verify_returnsBadRequest_whenServiceThrows() throws Exception {
            VerifyUserDTO req = new VerifyUserDTO("test@example.com", "123456");
            when(authenticationService.verifyUser(any(VerifyUserDTO.class)))
                    .thenThrow(new RuntimeException("Invalid verification code"));

            mockMvc.perform(post("/api/v1/auth/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid verification code"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify returns 400 for invalid payload")
        void verify_returnsBadRequest_whenInvalidPayload() throws Exception {
            String invalidJson = "{\"email\":\"bad-email\",\"verificationCode\":\"1\"}";

            mockMvc.perform(post("/api/v1/auth/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ResendVerificationTests {

        @Test
        @DisplayName("POST /api/v1/auth/verify/resend returns 200 for valid email")
        void resend_returnsOk_whenValid() throws Exception {
            String email = "test@example.com";
            doNothing().when(authenticationService).resendVerificationCode(email);

            mockMvc.perform(post("/api/v1/auth/verify/resend")
                            .param("email", email))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Verification code sent"));

            verify(authenticationService).resendVerificationCode(email);
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify/resend returns 400 when service throws")
        void resend_returnsBadRequest_whenServiceThrows() throws Exception {
            String email = "missing@example.com";
            org.mockito.Mockito.doThrow(new RuntimeException("User not found"))
                    .when(authenticationService).resendVerificationCode(email);

            mockMvc.perform(post("/api/v1/auth/verify/resend")
                            .param("email", email))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("User not found"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify/resend returns 400 when email param missing")
        void resend_returnsBadRequest_whenEmailMissing() throws Exception {
            // Missing required \`email\` request param leads to 400 by Spring MVC
            mockMvc.perform(post("/api/v1/auth/verify/resend"))
                    .andExpect(status().isBadRequest());
        }
    }
}