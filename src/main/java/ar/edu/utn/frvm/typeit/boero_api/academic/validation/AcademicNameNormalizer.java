package ar.edu.utn.frvm.typeit.boero_api.academic.validation;

public final class AcademicNameNormalizer {

  private AcademicNameNormalizer() {}

  public static String display(final String value) {
    return value.trim().replaceAll("\\s+", " ");
  }

  public static String search(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
