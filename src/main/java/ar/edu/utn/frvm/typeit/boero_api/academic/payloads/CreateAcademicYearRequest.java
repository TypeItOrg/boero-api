package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record CreateAcademicYearRequest(
    @Min(value = 2000, message = "El año debe ser igual o posterior a 2000.") int year,
    LocalDate startDate,
    LocalDate endDate) {}
