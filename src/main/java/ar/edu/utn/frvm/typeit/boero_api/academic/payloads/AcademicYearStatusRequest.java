package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import jakarta.validation.constraints.NotNull;

public record AcademicYearStatusRequest(@NotNull AcademicYearStatus status) {}
