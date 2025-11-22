package com.wellwork.dto;

import com.wellwork.model.enums.EnergyLevel;
import com.wellwork.model.enums.Mood;
import lombok.Data;

import java.time.Instant;
import java.time.OffsetDateTime;

@Data
public class CheckInResponseDTO {

    private Long id;
    private Long userId;
    private Mood mood;
    private EnergyLevel energyLevel;
    private String notes;
    private OffsetDateTime createdAt;
    private String generatedMessage;
}
