package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.constraints.NotNull;

public record ActiveStatusRequest(@NotNull Boolean active) {}
