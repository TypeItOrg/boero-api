package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRoleRequest(@NotNull(message = "El rol es requerido.") UUID roleId) {}
