package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "El token de actualización es requerido.") String refreshToken) {}
