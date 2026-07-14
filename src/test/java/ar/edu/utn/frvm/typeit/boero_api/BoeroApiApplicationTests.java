package ar.edu.utn.frvm.typeit.boero_api;

import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@IntegrationTest
class BoeroApiApplicationTests {

  @Container static final GenericContainer<?> REDIS = createRedisContainer();

  @SuppressWarnings("resource")
  private static GenericContainer<?> createRedisContainer() {
    return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
  }

  @DynamicPropertySource
  static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
  }

  @Test
  void contextLoads() {}
}
