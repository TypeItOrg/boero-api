package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ActiveSessionResponse(
    UUID sessionId,
    String ipAddress,
    String userAgent,
    LocalDateTime startedAt,
    boolean currentSession) {}
