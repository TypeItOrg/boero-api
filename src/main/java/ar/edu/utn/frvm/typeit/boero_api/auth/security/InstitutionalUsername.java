package ar.edu.utn.frvm.typeit.boero_api.auth.security;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class InstitutionalUsername {

  public record Parts(UUID institutionId, String documentNumber) {}

  private static final String SEPARATOR = ":";

  private InstitutionalUsername() {}

  public static String format(UUID institutionId, String documentNumber) {
    return institutionId + SEPARATOR + documentNumber;
  }

  public static Parts parse(String username) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("El nombre de usuario es requerido.");
    }

    int sep = username.indexOf(SEPARATOR);
    if (sep < 0 || sep == username.length() - 1) {
      throw new IllegalArgumentException("El nombre de usuario institucional no es válido.");
    }

    String institutionPart = username.substring(0, sep);
    String documentPart = username.substring(sep + 1);
    return new Parts(UUID.fromString(institutionPart), documentPart);
  }
}
