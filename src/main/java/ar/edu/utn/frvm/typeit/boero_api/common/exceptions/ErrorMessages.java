package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

public final class ErrorMessages {
  public static final String INTERNAL_SERVER_ERROR_MESSAGE =
      "Se ha producido un error interno en el servidor.";

  public static final String VALIDATION_ERROR_MESSAGE = "Se encontraron errores de validación.";
  public static final String MALFORMED_REQUEST_BODY =
      "El formato del cuerpo de la solicitud es inválido.";

  public static final String UNHANDLED_EXCEPTION_MESSAGE =
      "[Exception] Ocurrió un error inesperado: {}";

  private ErrorMessages() {}
}
