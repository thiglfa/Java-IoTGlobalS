package com.wellwork.controller;

import com.wellwork.dto.UserLoginDTO;
import com.wellwork.dto.UserRequestDTO;
import com.wellwork.dto.UserResponseDTO;
import com.wellwork.service.AuthService;
import com.wellwork.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public record LoginResponse(String token) {}

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register( @RequestBody UserRequestDTO req) {
        UserResponseDTO created = userService.create(req);
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody UserLoginDTO req) {
        String token = authService.login(req.getUsername(), req.getPassword());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
