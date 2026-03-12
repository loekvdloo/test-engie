package nl.engie.allocation.dto;

/**
 * DTO for a single validation error code returned in a NACK response.
 */
public record ValidationErrorDto(
        String code,
        String message,
        String ruleCode
) {}
