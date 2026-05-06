package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import java.util.UUID;

public record UserRegisteredResponse(UUID userId, String documentNumber, UUID institutionId) {}
