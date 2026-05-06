package ar.edu.utn.frvm.typeit.boero_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BoeroApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(BoeroApiApplication.class, args);
  }
}
