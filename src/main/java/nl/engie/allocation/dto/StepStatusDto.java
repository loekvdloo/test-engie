package nl.engie.allocation.dto;

import java.time.LocalDateTime;

public record StepStatusDto(
        String stepCode,
        String stepName,
        String phaseName,
        int stepOrder,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String resultMessage,
        String errorMessage
) {}
