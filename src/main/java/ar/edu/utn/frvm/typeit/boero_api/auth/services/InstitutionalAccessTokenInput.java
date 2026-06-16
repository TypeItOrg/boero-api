package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import java.util.UUID;
import lombok.Builder;

@Builder
public record InstitutionalAccessTokenInput(
    UUID userId, UUID personId, UUID institutionId, String documentNumber, UUID sessionId) {}
