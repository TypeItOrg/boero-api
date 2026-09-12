package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartEnrollmentApplicationRequest {

  @NotNull(message = "El plan de estudio es obligatorio")
  private UUID studyPlanId;

  @NotNull(message = "El ciclo lectivo es obligatorio")
  private UUID academicYearId;
}
