package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.constraints.Size;

public record AcademicLifecycleRequest(@Size(max = 500) String reason) {}
