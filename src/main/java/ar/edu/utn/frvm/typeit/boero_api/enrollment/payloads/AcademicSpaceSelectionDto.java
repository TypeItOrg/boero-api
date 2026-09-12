package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSpaceSelectionDto {
  @Builder.Default private List<UUID> studyPlanSpaceIds = new ArrayList<>();
}
