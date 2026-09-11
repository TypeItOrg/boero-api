package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;

public record PasskeyAuthenticationOptionsRequest(
    @NotBlank(message = ValidationMessages.LOGIN_ATTEMPT_REQUIRED) String loginAttemptId) {}
