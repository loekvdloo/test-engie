package nl.engie.allocation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MessageStatusResponse(
        String messageUuid,
        String messageType,
        String status,
        String currentStep,
        LocalDateTime receivedAt,
        LocalDateTime completedAt,
        Integer priority,
        String responseType,
        String responseXml,
        List<StepStatusDto> steps,
        List<ValidationErrorDto> errorCodes
) {}
