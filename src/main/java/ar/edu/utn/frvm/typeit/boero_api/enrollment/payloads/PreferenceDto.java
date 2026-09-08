package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceDto {
  private String preferredShift;
  private Boolean allowsImageUse;
  private Boolean isReenrolling;
  private String previousTeacher;
}
