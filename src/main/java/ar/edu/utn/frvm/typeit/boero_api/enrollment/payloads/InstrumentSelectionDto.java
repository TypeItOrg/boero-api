package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import java.util.HashMap;
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
public class InstrumentSelectionDto {
  @Builder.Default private Map<UUID, UUID> studyPlanSpaceInstrumentIds = new HashMap<>();
}
