package com.taskpilot.controller;

import com.taskpilot.dto.auth.*;
import com.taskpilot.exception.TokenRefreshException;
import com.taskpilot.model.User;
import com.taskpilot.service.AuthenticationService;
import com.taskpilot.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/auth")
@RestController
public class AuthenticationController {
    private final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final UserDetailsService userDetailsService;


    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterUserDTO registerUserDTO) {
        authenticationService.signup(registerUserDTO);
        return ResponseEntity.ok("User registered successfully. Please check your email.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticate(@Valid @RequestBody LoginUserDTO loginUserDTO) {
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        logger.info("jwtToken: {}", jwtToken);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);
        logger.info("refreshToken: {}", refreshToken);
        return ResponseEntity.ok(new LoginResponseDTO(jwtToken, refreshToken));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@Valid @RequestBody VerifyUserDTO verifyUserDTO) {
        try {
            User verifiedUser = authenticationService.verifyUser(verifyUserDTO);
            String jwtToken = jwtService.generateToken(verifiedUser);
            String refreshToken = jwtService.generateRefreshToken(verifiedUser);
            return ResponseEntity.ok(new LoginResponseDTO(jwtToken, refreshToken));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<?> resendVerificationCode( @NotNull @RequestParam  String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequestDTO request) {
        String requestRefreshToken = request.refreshToken();

        try {
            String username = jwtService.extractUsername(requestRefreshToken);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(requestRefreshToken, userDetails)) {
                // --- REFRESH TOKEN ROTATION ---
                // A new Access Token is generated
                String newAccessToken = jwtService.generateToken(userDetails);
                // A new Refresh Token is also generated
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);
                // Both new tokens are returned to the client
                return ResponseEntity.ok(new TokenRefreshResponseDTO(newAccessToken, newRefreshToken));
            } else {
                throw new TokenRefreshException(requestRefreshToken, "Refresh token is not valid!");
            }
        } catch (Exception e) {
            throw new TokenRefreshException(requestRefreshToken, e.getMessage());
        }
    }
}

