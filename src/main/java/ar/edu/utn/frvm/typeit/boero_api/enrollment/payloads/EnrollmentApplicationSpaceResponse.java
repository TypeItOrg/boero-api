package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(
    requiredProperties = {
      "studyPlanSpaceId",
      "spaceName",
      "academicLevelName",
      "instrumentId",
      "instrumentName"
    })
public record EnrollmentApplicationSpaceResponse(
    @Schema(nullable = true) UUID studyPlanSpaceId,
    @Schema(nullable = true) String spaceName,
    @Schema(nullable = true) String academicLevelName,
    @Schema(nullable = true) UUID instrumentId,
    @Schema(nullable = true) String instrumentName) {
  public EnrollmentApplicationSpaceResponse() {
    this(null, null, null, null, null);
  }

  public UUID getStudyPlanSpaceId() {
    return studyPlanSpaceId;
  }

  public String getSpaceName() {
    return spaceName;
  }

  public String getAcademicLevelName() {
    return academicLevelName;
  }

  public UUID getInstrumentId() {
    return instrumentId;
  }

  public String getInstrumentName() {
    return instrumentName;
  }
}
