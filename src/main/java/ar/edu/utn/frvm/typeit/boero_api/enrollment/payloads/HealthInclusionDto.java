package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(requiredProperties = {"receivesReasonableAdjustments", "adjustmentDetails"})
public record HealthInclusionDto(
    @Schema(nullable = true) Boolean receivesReasonableAdjustments,
    @Schema(nullable = true) String adjustmentDetails) {
  public HealthInclusionDto() {
    this(null, null);
  }

  public Boolean getReceivesReasonableAdjustments() {
    return receivesReasonableAdjustments;
  }

  public String getAdjustmentDetails() {
    return adjustmentDetails;
  }
}
