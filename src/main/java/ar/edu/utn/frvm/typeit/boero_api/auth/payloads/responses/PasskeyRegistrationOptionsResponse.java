package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
public record PasskeyRegistrationOptionsResponse(String ceremonyId, JsonNode options) {}
