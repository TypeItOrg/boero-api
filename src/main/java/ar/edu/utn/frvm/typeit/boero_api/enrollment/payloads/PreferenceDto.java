package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    requiredProperties = {"preferredShift", "allowsImageUse", "isReenrolling", "previousTeacher"})
public record PreferenceDto(
    @Schema(nullable = true) String preferredShift,
    @Schema(nullable = true) Boolean allowsImageUse,
    @Schema(nullable = true) Boolean isReenrolling,
    @Schema(nullable = true) String previousTeacher) {
  public PreferenceDto() {
    this(null, null, null, null);
  }

  public String getPreferredShift() {
    return preferredShift;
  }

  public Boolean getAllowsImageUse() {
    return allowsImageUse;
  }

  public Boolean getIsReenrolling() {
    return isReenrolling;
  }

  public String getPreviousTeacher() {
    return previousTeacher;
  }
}
