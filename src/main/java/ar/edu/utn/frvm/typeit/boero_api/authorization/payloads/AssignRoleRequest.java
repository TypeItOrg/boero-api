package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRoleRequest(@NotNull(message = ValidationMessages.ROLE_REQUIRED) UUID roleId) {}
