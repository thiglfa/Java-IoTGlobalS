package com.wellwork.controller;

import com.wellwork.dto.UserRequestDTO;
import com.wellwork.dto.UserResponseDTO;
import com.wellwork.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // get current authenticated user's profile
    @GetMapping("/me")
    public UserResponseDTO me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // ← isto sempre funciona no seu sistema

        return userService.findByUsernameResponse(username);
    }

    // list users (admin use-case) - paginated
    @GetMapping("/all")
    public ResponseEntity<Page<UserResponseDTO>> list(Pageable pageable) {
        Page<UserResponseDTO> p = userService.list(pageable);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable("id") Long id) {
        UserResponseDTO dto = userService.getById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable("id") Long id,
            @Valid @RequestBody String newPassword) {
        userService.updatePassword(id, newPassword);
        return ResponseEntity.noContent().build();
    }

    // create user (if needed outside /auth/register)
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO created = userService.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
