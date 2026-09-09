package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerSelectionDto {
  private UUID trainingPathId;
}
