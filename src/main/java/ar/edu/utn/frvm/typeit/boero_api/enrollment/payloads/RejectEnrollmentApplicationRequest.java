package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectEnrollmentApplicationRequest(
    @NotBlank @Size(max = 1000) String rejectionReason) {}
