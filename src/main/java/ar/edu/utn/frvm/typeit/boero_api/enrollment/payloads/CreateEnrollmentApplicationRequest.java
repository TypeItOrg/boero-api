package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEnrollmentApplicationRequest(
    @NotNull UUID studyPlanId, @NotNull UUID academicYearId) {}
