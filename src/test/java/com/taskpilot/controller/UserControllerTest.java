package com.taskpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.config.JwtAuthenticationFilter;
import com.taskpilot.config.SecurityConfiguration;
import com.taskpilot.model.Plan;
import com.taskpilot.model.User;
import com.taskpilot.service.JwtService;
import com.taskpilot.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, UserControllerTest.ValidationConfig.class})
class UserControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer test-token";
    private static final String RAW_TOKEN = "test-token";
    private static final String USER_EMAIL = "test@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Security collaborators
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    // Controller collaborator
    @MockitoBean
    private UserService userService;

    // Principal that is both domain User and UserDetails
    private User currentUser;

    @TestConfiguration
    static class ValidationConfig {
        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @BeforeEach
    void setupSecurity() {
        when(jwtService.extractUsername(RAW_TOKEN)).thenReturn(USER_EMAIL);

        // Mock domain User that also implements UserDetails for SecurityContext principal
        currentUser = Mockito.mock(User.class, withSettings().extraInterfaces(UserDetails.class));
        UserDetails principal = currentUser;

        when(principal.getUsername()).thenReturn(USER_EMAIL);

        // Fix: return a value typed as Collection<? extends GrantedAuthority>
        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(currentUser).getAuthorities();

        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(principal);
        when(jwtService.isTokenValid(eq(RAW_TOKEN), any(UserDetails.class))).thenReturn(true);

        // Domain fields used by controller
        when(currentUser.getId()).thenReturn(123L);
        when(currentUser.getEmail()).thenReturn(USER_EMAIL);
        when(currentUser.isEnabled()).thenReturn(true);
        when(currentUser.getNotionWorkspaceName()).thenReturn("My WS");
        when(currentUser.getNotionWorkspaceIcon()).thenReturn("icon");
        when(currentUser.getNotionTargetDatabaseId()).thenReturn("db-1");
        when(currentUser.getRequestsInCurrentDay()).thenReturn(2);
        when(currentUser.getRequestsInCurrentMonth()).thenReturn(10);
        when(currentUser.getPlanRefreshDate()).thenReturn(LocalDate.of(2025, 1, 1));

        Plan plan = new Plan("Free", 50, 5, List.of());
        when(currentUser.getPlan()).thenReturn(plan);

        // For endpoints that look up the user by email
        when(userService.findUserByEmail(USER_EMAIL)).thenReturn(Optional.of(currentUser));
    }

    // GET /api/v1/users/me (requires auth)
    @Test
    @DisplayName("GET /api/v1/users/me returns 200 with current user DTO")
    void getCurrentUser_returnsOk_withUserDto() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123L))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.notionTargetDatabaseId").value("db-1"))
                .andExpect(jsonPath("$.requestsInCurrentDay").value(2))
                .andExpect(jsonPath("$.requestsInCurrentMonth").value(10))
                .andExpect(jsonPath("$.plan.name").value("Free"))
                .andExpect(jsonPath("$.plan.requestsPerDay").value(5))
                .andExpect(jsonPath("$.plan.requestsPerMonth").value(50));
    }

    // GET /api/v1/users/enabled/{email} (public)
    @Test
    @DisplayName("GET /api/v1/users/enabled/{email} returns 200 with enabled status when found")
    void getUserEnabled_returnsOk_whenFound() throws Exception {
        when(userService.findUserByEmail("a@b.com")).thenReturn(Optional.of(currentUser));
        when(currentUser.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/enabled/{email}", "a@b.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/users/enabled/{email} returns 404 when not found")
    void getUserEnabled_returnsNotFound_whenMissing() throws Exception {
        when(userService.findUserByEmail("missing@b.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/enabled/{email}", "missing@b.com"))
                .andExpect(status().isNotFound());
    }

    // PUT /api/v1/users/me/notion-database (requires auth)
    @Test
    @DisplayName("PUT /api/v1/users/me/notion-database returns 200 when valid payload")
    void setNotionDatabase_returnsOk_whenValid() throws Exception {
        String payload = "{\"databaseId\":\"db-123\",\"databaseName\":\"Work DB\"}";

        mockMvc.perform(put("/api/v1/users/me/notion-database")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Database selection saved successfully."));

        verify(userService).setNotionTargetDatabase(123L, "db-123", "Work DB");
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/notion-database returns 400 for invalid payload")
    void setNotionDatabase_returnsBadRequest_whenInvalidPayload() throws Exception {
        String invalidJson = "{\"databaseId\":\"\",\"databaseName\":\"\"}";

        mockMvc.perform(put("/api/v1/users/me/notion-database")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/notion-database returns 500 when service throws")
    void setNotionDatabase_returnsServerError_whenServiceThrows() throws Exception {
        doThrow(new RuntimeException("failure"))
                .when(userService).setNotionTargetDatabase(anyLong(), anyString(), anyString());

        String payload = "{\"databaseId\":\"db-123\",\"databaseName\":\"Work DB\"}";

        mockMvc.perform(put("/api/v1/users/me/notion-database")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to save selection due to an internal error."));
    }
}