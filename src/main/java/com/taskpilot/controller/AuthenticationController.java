package com.taskpilot.controller;

import com.taskpilot.dto.auth.LoginResponseDTO;
import com.taskpilot.dto.auth.LoginUserDTO;
import com.taskpilot.dto.auth.RegisterUserDTO;
import com.taskpilot.dto.auth.VerifyUserDTO;
import com.taskpilot.model.User;
import com.taskpilot.service.AuthenticationService;
import com.taskpilot.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterUserDTO registerUserDTO) {
        User registeredUser = authenticationService.signup(registerUserDTO);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticate(@Valid @RequestBody LoginUserDTO loginUserDTO) {
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        return ResponseEntity.ok(new LoginResponseDTO(jwtToken));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@Valid @RequestBody VerifyUserDTO verifyUserDTO) {
        try {
            User verifiedUser = authenticationService.verifyUser(verifyUserDTO);
            String jwtToken = jwtService.generateToken(verifiedUser);
            return ResponseEntity.ok(new LoginResponseDTO(jwtToken));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // For token-based auth, logout is handled client-side.
        // This endpoint can remain for semantic purposes.
        return ResponseEntity.ok("Logout successful");
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}