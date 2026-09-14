package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(requiredProperties = {"trainingPathId"})
public record CareerSelectionDto(@Schema(nullable = true) UUID trainingPathId) {
  public CareerSelectionDto() {
    this(null);
  }

  public UUID getTrainingPathId() {
    return trainingPathId;
  }
}
