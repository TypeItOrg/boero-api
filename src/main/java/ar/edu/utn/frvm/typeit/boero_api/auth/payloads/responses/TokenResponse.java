package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import lombok.Builder;

@Builder
public record TokenResponse(String accessToken, String refreshToken) {}
