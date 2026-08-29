package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentApplicationResponse {

  private UUID applicationId;
  private UUID institutionId;
  private UUID personId;
  private UUID studyPlanId;
  private UUID academicYearId;
  private UUID enrollmentPeriodId;
  private EnrollmentApplicationStatus status;
  private boolean isEditable;
  private Map<String, Object> data;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
