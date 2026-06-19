package ar.edu.utn.frvm.typeit.boero_api.common.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

public final class SearchNormalization {

  public static final String FROM = "áéíóúÁÉÍÓÚüÜñÑ";
  public static final String TO = "aeiouAEIOUuUnN";

  public static final String UNACCENT_LOWER_FUNCTION = "unaccent_lower";
  public static final String UNACCENT_LOWER_PATTERN =
      "lower(translate(?1, '" + FROM + "', '" + TO + "'))";

  public static Expression<String> unaccentLower(
      final CriteriaBuilder builder, final Expression<String> expression) {
    return builder.function(UNACCENT_LOWER_FUNCTION, String.class, expression);
  }
}
