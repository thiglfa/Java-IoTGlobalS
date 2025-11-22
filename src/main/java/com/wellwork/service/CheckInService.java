package com.wellwork.service;

import com.wellwork.dto.CheckInRequestDTO;
import com.wellwork.dto.CheckInResponseDTO;
import com.wellwork.model.entities.CheckIn;
import com.wellwork.model.entities.User;
import com.wellwork.repository.CheckInRepository;
import com.wellwork.repository.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final GeneratedMessageService generatedMessageService;

    public CheckInService(CheckInRepository checkInRepository,
                          UserRepository userRepository,
                          GeneratedMessageService generatedMessageService) {
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
        this.generatedMessageService = generatedMessageService;
    }

    @Transactional
    public CheckInResponseDTO updatePartial(Long id, Long userId, CheckInRequestDTO dto) {
        CheckIn checkIn = checkInRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CheckIn não encontrado: " + id));

        if (dto.getMood() != null) {
            checkIn.updateMood(dto.getMood());
        }

        if (dto.getEnergyLevel() != null) {
            checkIn.updateEnergy(dto.getEnergyLevel());
        }

        if (dto.getNotes() != null) {
            checkIn.updateNotes(dto.getNotes());
        }

        checkInRepository.save(checkIn);

        return toResponseDTO(checkIn);
    }


    @Transactional
    public CheckInResponseDTO create(CheckInRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + dto.getUserId()));

        CheckIn checkIn = new CheckIn();
        checkIn.setUser(user);
        checkIn.setMood(dto.getMood());
        checkIn.setEnergyLevel(dto.getEnergyLevel());
        checkIn.setNotes(dto.getNotes());
        checkIn = checkInRepository.save(checkIn);

        // Dispara geração assíncrona de mensagem
        generateAiMessageAsync(checkIn.getId());

        return toResponseDTO(checkIn);
    }

    public Page<CheckInResponseDTO> findByUser(Long userId, Pageable pageable) {
        return checkInRepository.findByUserId(userId, pageable).map(this::toResponseDTO);
    }

    public CheckIn findEntityById(Long id) {
        return checkInRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("CheckIn não encontrado: " + id));
    }

    public CheckInResponseDTO toResponseDTO(CheckIn ck) {
        CheckInResponseDTO dto = new CheckInResponseDTO();
        dto.setId(ck.getId());
        dto.setUserId(ck.getUser().getId());
        dto.setMood(ck.getMood());
        dto.setEnergyLevel(ck.getEnergyLevel());
        dto.setNotes(ck.getNotes());
        dto.setCreatedAt(ck.getCreatedAt());
        if (ck.getGeneratedMessage() != null) {
            dto.setGeneratedMessage(ck.getGeneratedMessage().getMessage());
        } else {
            dto.setGeneratedMessage(null);
        }
        return dto;
    }

    @Async("taskExecutor")
    public void generateAiMessageAsync(Long checkInId) {
        try {
            generatedMessageService.generateForCheckIn(checkInId);
        } catch (Exception ex) {
            // log error but do not fail the main transaction
            // use a logger in real code: logger.error("Failed AI generation", ex);
            System.err.println("Falha ao gerar mensagem AI para checkIn " + checkInId + ": " + ex.getMessage());
        }
    }
}
