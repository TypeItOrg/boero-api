package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

public final class AuthMessages {
  public static final String INVALID_CREDENTIALS = "Las credenciales proporcionadas son inválidas.";
  public static final String USER_DISABLED = "El usuario se encuentra deshabilitado.";
  public static final String USER_ALREADY_EXISTS =
      "Ya existe un usuario con este documento en la institución especificada.";
  public static final String INSTITUTION_NOT_FOUND = "La institución especificada no existe.";
  public static final String TOKEN_EXPIRED = "El token de acceso ha expirado.";
  public static final String TOKEN_INVALID = "El token de acceso es inválido.";
  public static final String TOKEN_MISSING =
      "Se requiere el header Authorization con un Bearer token.";
  public static final String TOKEN_BLACKLISTED = "El token ha sido revocado.";
  public static final String TOKEN_SESSION_INACTIVE = "La sesión asociada al token no está activa.";
  public static final String REFRESH_TOKEN_INVALID = "El refresh token es inválido o ha expirado.";
  public static final String REFRESH_TOKEN_REUSE =
      "Se detectó reutilización del refresh token. Todas las sesiones fueron revocadas.";
  public static final String PLATFORM_ACCOUNT_DISABLED =
      "La cuenta de plataforma se encuentra deshabilitada.";

  private AuthMessages() {}
}
