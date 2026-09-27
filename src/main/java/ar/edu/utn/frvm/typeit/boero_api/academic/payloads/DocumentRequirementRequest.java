package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DocumentRequirementRequest(
    @NotBlank @Size(max = 150) String name,
    @NotNull @Size(max = 1000) String instructions,
    @NotNull DocumentRequirementLevel level,
    @NotEmpty
        List<@NotBlank @Pattern(regexp = "application/pdf|image/jpeg|image/png") String>
            allowedFormats,
    @Min(0) int displayOrder,
    @NotNull Boolean active) {}
