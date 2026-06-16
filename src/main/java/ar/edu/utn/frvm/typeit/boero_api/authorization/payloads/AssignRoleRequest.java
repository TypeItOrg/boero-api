package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(@NotNull(message = "El rol es requerido.") SystemRoleCode role) {}
