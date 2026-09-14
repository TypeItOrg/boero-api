package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(requiredProperties = {"studyPlanSpaceInstrumentIds"})
public record InstrumentSelectionDto(
    @Schema(nullable = true) Map<UUID, UUID> studyPlanSpaceInstrumentIds) {
  public InstrumentSelectionDto() {
    this(new HashMap<>());
  }

  public InstrumentSelectionDto {
    studyPlanSpaceInstrumentIds =
        studyPlanSpaceInstrumentIds == null ? new HashMap<>() : studyPlanSpaceInstrumentIds;
  }

  public Map<UUID, UUID> getStudyPlanSpaceInstrumentIds() {
    return studyPlanSpaceInstrumentIds;
  }
}
