package com.taskpilot.controller;

import com.taskpilot.dto.notion.DatabaseInfoDTO;
import com.taskpilot.dto.user.ExchangeCodeDTO;
import com.taskpilot.exception.InvalidDatabaseSchemaException;
import com.taskpilot.exception.ResourceNotFoundException;
import com.taskpilot.model.User;
import com.taskpilot.service.NotionService;
import com.taskpilot.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notion")
public class NotionController {

    private final NotionService notionService;
    private final UserService userService; // Assuming you have a method to find user by email
    private final Logger logger = LoggerFactory.getLogger(NotionController.class);

    public NotionController(NotionService notionService, UserService userService) {
        this.notionService = notionService;
        this.userService = userService;
    }

    @PostMapping("/exchange-code")
    public ResponseEntity<?> exchangeCodeForToken(
            @Valid @RequestBody ExchangeCodeDTO request,
            @AuthenticationPrincipal UserDetails userDetails // Get the currently authenticated principal
    ) {

        try {
            // Find the full User entity from the principal's username (which is the email)
            User currentUser = userService.findUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));

            // Pass the code and user to the service
            notionService.exchangeCodeAndSaveToken(request.code(), currentUser);

            return ResponseEntity.ok().body(Map.of("message", "Notion account connected successfully."));
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error during Notion OAuth exchange: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to connect Notion account.");
        }
    }

    @GetMapping("/databases")
    public ResponseEntity<?> listDatabases(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.findUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

            // Check if the user has connected their Notion account first
            if (currentUser.getNotionAccessToken() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Notion account not connected."));
            }

            List<DatabaseInfoDTO> databases = notionService.getAvailableDatabases(currentUser.getNotionAccessToken());
            return ResponseEntity.ok(databases);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve databases."));
        }
    }


    @PostMapping("/taskList/{id}")
    public ResponseEntity<?> createTaskListPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        try {
            User currentUser = userService.findUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

            // Check if the user has connected Notion and selected a database
            if (currentUser.getNotionAccessToken() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Notion account not connected."));
            }

            if (currentUser.getNotionTargetDatabaseId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No Notion database selected."));
            }

            notionService.createTaskListPage(id, currentUser);

            return ResponseEntity.ok(Map.of("message", "Task list exported to Notion successfully."));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (InvalidDatabaseSchemaException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create Notion page", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to export task list to Notion: " + e.getMessage()));
        }
    }

}