package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

public final class ErrorMessages {
  public static final String INTERNAL_SERVER_ERROR_MESSAGE =
      "Se ha producido un error interno en el servidor.";

  public static final String VALIDATION_ERROR_MESSAGE = "Se encontraron errores de validación.";

  public static final String UNHANDLED_EXCEPTION_MESSAGE = "Ocurrió un error inesperado: {}";

  private ErrorMessages() {}
}
