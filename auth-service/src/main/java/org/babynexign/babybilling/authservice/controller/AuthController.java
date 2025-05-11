package org.babynexign.babybilling.authservice.controller;

import jakarta.validation.Valid;
import org.babynexign.babybilling.authservice.dto.AuthResponse;
import org.babynexign.babybilling.authservice.dto.LoginRequest;
import org.babynexign.babybilling.authservice.dto.RegisterRequest;
import org.babynexign.babybilling.authservice.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
