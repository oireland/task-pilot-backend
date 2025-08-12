package com.taskpilot.controller;

import com.taskpilot.dto.user.PlanDTO;
import com.taskpilot.model.Plan;
import com.taskpilot.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    @GetMapping("/me")
    public ResponseEntity<PlanDTO> getCurrentUserPlan() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Plan currentPlan = currentUser.getPlan();

        PlanDTO planDTO = new PlanDTO(
                currentPlan.getName(),
                currentPlan.getRequestsPerDay(),
                currentPlan.getRequestsPerMonth()
        );

        return ResponseEntity.ok(planDTO);
    }
}