package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "app.storage.enrollment.provider", havingValue = "s3")
public class S3StorageConfiguration {
  @Bean(destroyMethod = "close")
  public S3Client enrollmentS3Client(
      @Value("${app.storage.enrollment.s3.region}") final String region) {
    return S3Client.builder().region(Region.of(region)).build();
  }
}
