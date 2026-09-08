package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnrollmentDraftRequest {

  private EnrollmentDraftData data;
}
