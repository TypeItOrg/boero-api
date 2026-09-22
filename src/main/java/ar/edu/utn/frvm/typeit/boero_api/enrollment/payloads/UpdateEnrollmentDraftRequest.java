package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Builder;

@Builder
public record UpdateEnrollmentDraftRequest(
    @Schema(nullable = true) @Valid EnrollmentDraftData data) {
  public UpdateEnrollmentDraftRequest() {
    this(null);
  }

  public EnrollmentDraftData getData() {
    return data;
  }
}
