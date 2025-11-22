package com.wellwork.controller;

import com.wellwork.dto.CheckInRequestDTO;
import com.wellwork.dto.CheckInResponseDTO;
import com.wellwork.dto.GeneratedMessageResponseDTO;
import com.wellwork.model.entities.GeneratedMessage;
import com.wellwork.service.CheckInService;
import com.wellwork.service.GeneratedMessageService;
import com.wellwork.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkins")
public class CheckInController {

    private final CheckInService checkInService;
    private final GeneratedMessageService generatedMessageService;
    private final UserService userService;

    public CheckInController(CheckInService checkInService,
                             GeneratedMessageService generatedMessageService,
                             UserService userService) {
        this.checkInService = checkInService;
        this.generatedMessageService = generatedMessageService;
        this.userService = userService;
    }

    // CREATE CHECK-IN
    @PostMapping
    public ResponseEntity<CheckInResponseDTO> create(Authentication authentication,
                                                     @Valid @RequestBody CheckInRequestDTO dto) {

        String username = authentication.getName();
        Long userId = userService.findEntityByUsername(username).getId();

        dto.setUserId(userId);

        CheckInResponseDTO created = checkInService.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    // LIST CHECK-INS FROM LOGGED USER
    @GetMapping
    public ResponseEntity<Page<CheckInResponseDTO>> listMine(Authentication authentication,
                                                             Pageable pageable) {

        String username = authentication.getName();
        Long userId = userService.findEntityByUsername(username).getId();

        Page<CheckInResponseDTO> page = checkInService.findByUser(userId, pageable);
        return ResponseEntity.ok(page);
    }

    // GET CHECK-IN BY ID
    @GetMapping("/{id}")
    public ResponseEntity<CheckInResponseDTO> getById(@PathVariable Long id) {
        CheckInResponseDTO dto = checkInService.toResponseDTO(checkInService.findEntityById(id));
        return ResponseEntity.ok(dto);
    }

    // PATCH (UPDATE PARTIAL)
    @PatchMapping("/{id}")
    public ResponseEntity<CheckInResponseDTO> patch(@PathVariable Long id,
                                                    @Valid @RequestBody CheckInRequestDTO patchDto,
                                                    Authentication authentication) {

        String username = authentication.getName();
        Long userId = userService.findEntityByUsername(username).getId();

        CheckInResponseDTO updated = checkInService.updatePartial(id, userId, patchDto);
        return ResponseEntity.ok(updated);
    }

    // GENERATE AI MESSAGE
    @PostMapping("/{id}/generate-message")
    public ResponseEntity<GeneratedMessageResponseDTO> generateMessage(@PathVariable("id") Long id,
                                                            Authentication authentication) {

        // Apenas garantindo que o check-in existe
        var checkIn = checkInService.findEntityById(id);

        // Gera a mensagem usando sua service
        GeneratedMessageResponseDTO response = generatedMessageService.generateForCheckIn(id);

        return ResponseEntity.status(201).body(response);
    }
}
