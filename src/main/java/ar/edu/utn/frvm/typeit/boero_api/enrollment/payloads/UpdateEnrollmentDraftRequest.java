package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UpdateEnrollmentDraftRequest(@Schema(nullable = true) EnrollmentDraftData data) {
  public UpdateEnrollmentDraftRequest() {
    this(null);
  }

  public EnrollmentDraftData getData() {
    return data;
  }
}
