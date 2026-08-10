package ar.edu.utn.frvm.typeit.boero_api.common.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SearchNormalization {

  private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

  private SearchNormalization() {}

  public static final String FROM = "áéíóúÁÉÍÓÚüÜñÑ";
  public static final String TO = "aeiouAEIOUuUnN";

  public static final String UNACCENT_LOWER_FUNCTION = "unaccent_lower";
  public static final String UNACCENT_LOWER_PATTERN =
      "lower(translate(?1, '" + FROM + "', '" + TO + "'))";

  public static Expression<String> unaccentLower(
      final CriteriaBuilder builder, final Expression<String> expression) {
    return builder.function(UNACCENT_LOWER_FUNCTION, String.class, expression);
  }

  public static String normalizeSearch(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public static String normalizeForComparison(final String value) {
    final String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
    return DIACRITICS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
  }

  public static String likeContainsPattern(final String value) {
    return "%" + escapeLike(normalizeForComparison(value)) + "%";
  }

  public static String escapeLike(final String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
