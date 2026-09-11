package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
public record IdentifyLoginResponse(String loginAttemptId, LoginNextStep nextStep) {

  public enum LoginNextStep {
    PASSWORD,
    PASSKEY
  }
}
