package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActiveSessionResponse(
    UUID sessionId,
    String ipAddress,
    String userAgent,
    LocalDateTime startedAt,
    boolean currentSession) {}
