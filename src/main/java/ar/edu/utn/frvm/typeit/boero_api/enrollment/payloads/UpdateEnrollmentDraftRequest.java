package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateEnrollmentDraftRequest(
    @Nullable @Size(max = 120) String firstName,
    @Nullable @Size(max = 120) String lastName,
    @Nullable @Size(max = 255) String secondarySchool) {}
