package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(requiredProperties = {"studyPlanSpaceIds"})
public record AcademicSpaceSelectionDto(@Schema(nullable = true) List<UUID> studyPlanSpaceIds) {
  public AcademicSpaceSelectionDto() {
    this(new ArrayList<>());
  }

  public AcademicSpaceSelectionDto {
    studyPlanSpaceIds = studyPlanSpaceIds == null ? new ArrayList<>() : studyPlanSpaceIds;
  }

  public List<UUID> getStudyPlanSpaceIds() {
    return studyPlanSpaceIds;
  }
}
