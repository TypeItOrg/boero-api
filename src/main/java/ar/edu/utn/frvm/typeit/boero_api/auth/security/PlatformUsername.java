package ar.edu.utn.frvm.typeit.boero_api.auth.security;

public final class PlatformUsername {

  private static final String PREFIX = "platform:";

  private PlatformUsername() {}

  public static String format(String email) {
    return PREFIX + email.trim().toLowerCase();
  }

  public static boolean isPlatformPrincipal(String principal) {
    return principal != null && principal.regionMatches(true, 0, PREFIX, 0, PREFIX.length());
  }

  public static String parseEmail(String principal) {
    if (!isPlatformPrincipal(principal)) {
      throw new IllegalArgumentException(
          "El formato del nombre de usuario de plataforma no es válido.");
    }

    return principal.substring(PREFIX.length());
  }
}
