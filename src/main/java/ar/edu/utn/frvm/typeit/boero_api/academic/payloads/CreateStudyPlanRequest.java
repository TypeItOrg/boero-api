package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateStudyPlanRequest(
    @NotBlank @Size(max = 150) String name, LocalDate effectiveFrom, LocalDate effectiveTo) {}
