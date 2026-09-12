package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
public record IdentifyLoginResponse(
    @Nullable @Schema(nullable = true) String loginAttemptId, LoginNextStep nextStep) {

  public enum LoginNextStep {
    EMAIL_VERIFICATION,
    PASSWORD,
    PASSKEY
  }
}
