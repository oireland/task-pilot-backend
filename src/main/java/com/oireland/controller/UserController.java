package com.oireland.controller;

import com.oireland.dto.UserDTO;
import com.oireland.model.User;
import com.oireland.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                currentUser.getNotionWorkspaceIcon()
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
}
