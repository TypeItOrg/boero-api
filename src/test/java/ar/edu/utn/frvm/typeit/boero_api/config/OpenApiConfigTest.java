package ar.edu.utn.frvm.typeit.boero_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {
  @Test
  void usesThePublicVersionPrefixInDocumentedPaths() {
    final OpenAPI openApi =
        new OpenAPI()
            .paths(
                new Paths()
                    .addPathItem("/api/1/institutions", new PathItem())
                    .addPathItem("/actuator/health", new PathItem()));

    new OpenApiConfig().publicApiVersionPaths().customise(openApi);

    assertThat(openApi.getPaths())
        .containsKeys("/api/v1/institutions", "/actuator/health")
        .doesNotContainKey("/api/1/institutions");
  }
}
