package com.taskpilot.dto.user;

import java.time.LocalDate;

// This DTO is used to safely transfer user data to the frontend.
public record UserDTO(
        Long id,
        String email,
        boolean enabled,
        String notionWorkspaceName,
        String notionWorkspaceIcon,
        String notionTargetDatabaseId,
        // Add usage stats here
        int requestsInCurrentDay,
        int requestsInCurrentMonth,
        LocalDate planRefreshDate,
        // Plan details
        PlanDTO plan
) {}