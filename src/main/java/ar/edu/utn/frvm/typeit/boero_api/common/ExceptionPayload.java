package ar.edu.utn.frvm.typeit.boero_api.common;

import lombok.Builder;

@Builder
public record ExceptionPayload(int status, String message) {}
