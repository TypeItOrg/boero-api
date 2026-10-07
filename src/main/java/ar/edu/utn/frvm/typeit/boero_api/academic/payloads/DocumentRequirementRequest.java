package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record DocumentRequirementRequest(
    @NotNull UUID documentId,
    @PositiveOrZero @Nullable Long revision,
    @NotNull DocumentRequirementLevel level,
    @Min(0) int displayOrder,
    @NotNull Boolean active,
    @Size(max = 1000) @Nullable String specificInstructions) {}
