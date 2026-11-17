package anurag.java.security.controller;

import anurag.java.security.service.AuthRequest;
import anurag.java.security.service.AuthResponse;
import anurag.java.security.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthError> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new AuthError("Invalid username or password"));
    }

    public record AuthError(String error) {
    }
}
