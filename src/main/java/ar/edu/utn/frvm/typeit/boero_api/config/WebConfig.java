package ar.edu.utn.frvm.typeit.boero_api.config;

import ar.edu.utn.frvm.typeit.boero_api.common.UnversionedRestController;
import java.util.function.Predicate;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private static final Predicate<Class<?>> VERSIONED_REST_CONTROLLERS =
      HandlerTypePredicate.forAnnotation(RestController.class)
          .and(
              controllerType ->
                  !AnnotatedElementUtils.hasAnnotation(
                      controllerType, UnversionedRestController.class));

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix("/api/{version}", VERSIONED_REST_CONTROLLERS);
  }

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    configurer.usePathSegment(1, WebConfig::IS_VERSIONED_API_PATH);
    configurer.setVersionRequired(false);
  }

  private static boolean IS_VERSIONED_API_PATH(RequestPath requestPath) {
    return requestPath.pathWithinApplication().value().startsWith("/api/");
  }
}
