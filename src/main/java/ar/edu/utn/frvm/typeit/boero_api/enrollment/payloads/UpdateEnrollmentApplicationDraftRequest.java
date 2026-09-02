package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record UpdateEnrollmentApplicationDraftRequest(@NotNull JsonNode data) {}
