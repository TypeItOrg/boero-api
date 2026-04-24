package ar.edu.utn.frvm.typeit.boero_api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("health-check")
public class HealthCheckController {
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public HealthCheckResponse healthCheck() {
    return HealthCheckResponse.builder().message("Application is healthy").build();
  }
}
