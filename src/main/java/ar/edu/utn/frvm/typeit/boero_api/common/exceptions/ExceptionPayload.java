package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExceptionPayload(int status, String message, Map<String, String> fieldErrors) {}
