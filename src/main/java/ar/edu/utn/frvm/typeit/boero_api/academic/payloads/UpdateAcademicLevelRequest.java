package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.ValidationMessages;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateAcademicLevelRequest(
    @Min(value = 1, message = ValidationMessages.ORDER_POSITIVE) int displayOrder,
    @Size(max = 1000) String description) {}
