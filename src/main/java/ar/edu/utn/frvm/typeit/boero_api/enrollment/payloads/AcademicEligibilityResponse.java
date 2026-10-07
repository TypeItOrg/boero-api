package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(requiredProperties = {"eligible", "requirements"})
public record AcademicEligibilityResponse(
    boolean eligible, List<AcademicRequirementResponse> requirements) {}
