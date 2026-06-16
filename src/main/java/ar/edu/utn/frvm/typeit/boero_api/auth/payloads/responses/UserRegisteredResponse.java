package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import java.util.UUID;
import lombok.Builder;

@Builder
public record UserRegisteredResponse(UUID userId, String documentNumber, UUID institutionId) {}
