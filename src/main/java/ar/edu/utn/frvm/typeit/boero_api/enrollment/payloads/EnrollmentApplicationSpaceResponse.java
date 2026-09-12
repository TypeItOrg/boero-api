package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentApplicationSpaceResponse {
  private UUID studyPlanSpaceId;
  private String spaceName;

  @Schema(nullable = true)
  private String academicLevelName;

  @Schema(nullable = true)
  private UUID instrumentId;

  @Schema(nullable = true)
  private String instrumentName;
}
