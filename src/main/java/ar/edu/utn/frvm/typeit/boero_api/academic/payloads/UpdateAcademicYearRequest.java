package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record UpdateAcademicYearRequest(
    @Min(value = 2000, message = "El año debe ser igual o posterior a 2000.") int year,
    LocalDate startDate,
    LocalDate endDate,
    AcademicYearStatus status) {}
