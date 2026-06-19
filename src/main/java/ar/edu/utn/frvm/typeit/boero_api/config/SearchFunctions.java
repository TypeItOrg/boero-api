package ar.edu.utn.frvm.typeit.boero_api.config;

import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;

public class SearchFunctions implements FunctionContributor {

  @Override
  public void contributeFunctions(final FunctionContributions contributions) {
    contributions
        .getFunctionRegistry()
        .registerPattern(
            SearchNormalization.UNACCENT_LOWER_FUNCTION,
            SearchNormalization.UNACCENT_LOWER_PATTERN);
  }
}
