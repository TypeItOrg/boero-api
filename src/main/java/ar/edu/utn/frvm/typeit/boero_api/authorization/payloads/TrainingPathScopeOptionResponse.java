package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(requiredProperties = {"id", "name"})
public record TrainingPathScopeOptionResponse(UUID id, String name) {}
