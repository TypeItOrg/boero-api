package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(requiredProperties = {"id", "status", "occurredAt", "actorId", "accountType"})
public record EnrollmentAdmissionHistoryResponse(
    UUID id,
    String status,
    Instant occurredAt,
    @Schema(nullable = true) UUID actorId,
    String accountType) {}
