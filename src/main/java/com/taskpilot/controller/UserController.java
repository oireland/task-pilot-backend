package com.taskpilot.controller;

import com.taskpilot.dto.user.SetDatabaseIdDTO;
import com.taskpilot.dto.user.UserDTO;
import com.taskpilot.model.User;
import com.taskpilot.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    // Your existing constructor and other methods...
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the details of the currently authenticated user.
     * The user is identified by the JWT token in the HttpOnly cookie.
     *
     * @return A ResponseEntity containing the UserDTO with safe user data.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        // The Authentication object is populated by the JwtAuthenticationFilter
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Map the User entity to a safe UserDTO
        UserDTO userDTO = new UserDTO(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.isEnabled(),
                currentUser.getNotionWorkspaceName(),
                currentUser.getNotionWorkspaceIcon(),
                currentUser.getNotionTargetDatabaseId(),
                currentUser.getNotionTargetDatabaseName()
        );

        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/enabled/{email}")
    public ResponseEntity<?> getUserEnabled(@PathVariable String email) {
        // Find the user by email using your service layer
        return userService.findUserByEmail(email)
                .map(user -> {
                    // If user is found, return their enabled status in a simple map
                    Map<String, Boolean> response = Map.of("enabled", user.isEnabled());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() ->
                        // If user is not found, return a 404 Not Found
                        ResponseEntity.notFound().build()
                );
    }

    @PutMapping("/me/notion-database")
    public ResponseEntity<?> setNotionDatabase(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SetDatabaseIdDTO request
    ) {
        try {
            User currentUser = userService.findUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found."));

            // Get the databaseId directly from the validated DTO
            userService.setNotionTargetDatabase(currentUser.getId(), request.databaseId(), request.databaseName());

            return ResponseEntity.ok(Map.of("message", "Database selection saved successfully."));
        } catch (Exception e) {
            // This now primarily catches internal errors, as validation is handled automatically
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save selection due to an internal error."));
        }
    }
}
