package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

public record UpdateInstitutionPublicAccessRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(required = true)
        @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        @Nullable String publicSubdomain) {}
