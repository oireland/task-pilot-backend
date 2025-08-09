package com.oireland.controller;

import com.oireland.dto.LoginUserDTO;
import com.oireland.dto.RegisterUserDTO;
import com.oireland.dto.VerifyUserDTO;
import com.oireland.model.User;
import com.oireland.service.AuthenticationService;
import com.oireland.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> register(@RequestBody RegisterUserDTO registerUserDTO) {
        User registeredUser = authenticationService.signup(registerUserDTO);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginUserDTO loginUserDTO, HttpServletResponse response) {
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);
        String jwtToken = jwtService.generateToken(authenticatedUser);

        // Create the cookie
        Cookie cookie = new Cookie("auth_token", jwtToken);
        cookie.setHttpOnly(true); // Prevents access from JavaScript
        cookie.setPath("/"); // Makes the cookie available for all paths
        cookie.setMaxAge((int) (jwtService.getExpirationTime() / 1000)); // Set cookie expiration in seconds
        cookie.setAttribute("SameSite", "Lax");

        // Only set the 'Secure' flag if we are NOT in the 'dev' profile
        if (!"dev".equals(activeProfile)) {
            cookie.setSecure(true); // Ensures the cookie is sent only over HTTPS
        }

        // Add the cookie to the response
        response.addCookie(cookie);

        // Return a success message or the user object (without the token)
        return ResponseEntity.ok("Login successful");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDTO verifyUserDTO) {
        try {
            authenticationService.verifyUser(verifyUserDTO);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
