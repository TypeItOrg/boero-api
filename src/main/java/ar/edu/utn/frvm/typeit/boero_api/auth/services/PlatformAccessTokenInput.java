package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import java.util.UUID;
import lombok.Builder;

@Builder
public record PlatformAccessTokenInput(UUID platformAccountId, String email, UUID sessionId) {}
