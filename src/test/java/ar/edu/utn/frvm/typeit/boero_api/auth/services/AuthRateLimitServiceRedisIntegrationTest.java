package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RateLimitExceededException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RateLimitUnavailableException;
import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@IntegrationTest
class AuthRateLimitServiceRedisIntegrationTest {

  private static final Duration WINDOW = Duration.ofSeconds(2);
  private static final int MAX_ATTEMPTS = 3;

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redisTemplate;
  private AuthRateLimitService service;

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
    try (final var connection =
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
      connection.serverCommands().flushDb();
    }
    service = new AuthRateLimitService(redisTemplate, rateLimitProperties());
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @Test
  @DisplayName("Should set TTL atomically on the first hit")
  void checkAllowed_setsTtlOnFirstHit() {
    service.checkAllowed("ttl-scope", "key", MAX_ATTEMPTS, WINDOW);

    final Long ttl = redisTemplate.getExpire("boero:auth:rate:ttl-scope:key");
    assertThat(ttl).isPositive().isLessThanOrEqualTo(WINDOW.toSeconds());
  }

  @Test
  @DisplayName("Should enforce the limit and reset after the window expires")
  void checkAllowed_enforcesLimitAndResetsAfterWindow() throws InterruptedException {
    service.checkAllowed("window-scope", "key", MAX_ATTEMPTS, WINDOW);
    service.checkAllowed("window-scope", "key", MAX_ATTEMPTS, WINDOW);
    service.checkAllowed("window-scope", "key", MAX_ATTEMPTS, WINDOW);

    assertThatThrownBy(() -> service.checkAllowed("window-scope", "key", MAX_ATTEMPTS, WINDOW))
        .isInstanceOf(RateLimitExceededException.class);

    Thread.sleep(WINDOW.toMillis() + 500);

    service.checkAllowed("window-scope", "key", MAX_ATTEMPTS, WINDOW);
  }

  @Test
  @DisplayName("Should count concurrent hits exactly once each")
  void checkAllowed_countsConcurrentHitsExactlyOnce() throws InterruptedException {
    final int threads = 10;
    final int callsPerThread = 20;
    final ExecutorService executor = Executors.newFixedThreadPool(threads);
    final CountDownLatch ready = new CountDownLatch(threads);
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(threads);

    for (int thread = 0; thread < threads; thread++) {
      executor.submit(
          () -> {
            ready.countDown();
            try {
              start.await();
              for (int call = 0; call < callsPerThread; call++) {
                service.checkAllowed(
                    "concurrent-scope", "key", Integer.MAX_VALUE, Duration.ofMinutes(1));
              }
            } catch (final InterruptedException exception) {
              Thread.currentThread().interrupt();
            } finally {
              done.countDown();
            }
          });
    }

    ready.await();
    start.countDown();
    assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
    executor.shutdown();

    assertThat(redisTemplate.opsForValue().get("boero:auth:rate:concurrent-scope:key"))
        .isEqualTo(String.valueOf(threads * callsPerThread));
    assertThat(redisTemplate.getExpire("boero:auth:rate:concurrent-scope:key")).isPositive();
  }

  @Test
  @DisplayName("Should keep buckets independent")
  void checkAllowed_keepsBucketsIndependent() {
    service.checkAllowed("scope-a", "key", 1, WINDOW);

    assertThatThrownBy(() -> service.checkAllowed("scope-a", "key", 1, WINDOW))
        .isInstanceOf(RateLimitExceededException.class);

    service.checkAllowed("scope-b", "key", 1, WINDOW);
  }

  @Test
  @DisplayName("Should derive HMAC keys that differ from plain SHA-256")
  void hashKey_differsFromPlainSha256() throws Exception {
    final String input = "institution|12345678";
    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
    final String plain =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));

    assertThat(service.hashKey(input)).isNotEqualTo(plain).doesNotContain("12345678");
  }

  @Test
  @DisplayName("Should fail closed when Redis is unreachable")
  void checkAllowed_failsClosedWhenRedisIsUnreachable() throws IOException {
    final int closedPort;
    try (final ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    final LettuceClientConfiguration clientConfiguration =
        LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(2))
            .shutdownTimeout(Duration.ZERO)
            .build();
    final LettuceConnectionFactory unreachableFactory =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("127.0.0.1", closedPort), clientConfiguration);
    unreachableFactory.afterPropertiesSet();
    final StringRedisTemplate unreachableTemplate = new StringRedisTemplate(unreachableFactory);
    unreachableTemplate.afterPropertiesSet();
    final AuthRateLimitService unreachableService =
        new AuthRateLimitService(unreachableTemplate, rateLimitProperties());

    try {
      assertThatThrownBy(
              () -> unreachableService.checkAllowed("scope", "key", MAX_ATTEMPTS, WINDOW))
          .isInstanceOf(RateLimitUnavailableException.class);
    } finally {
      unreachableFactory.destroy();
    }
  }

  private static AuthRateLimitProperties rateLimitProperties() {
    return new AuthRateLimitProperties(
        "integration-test-secret",
        30,
        Duration.ofMinutes(1),
        10,
        Duration.ofMinutes(1),
        10,
        Duration.ofMinutes(1),
        30,
        Duration.ofMinutes(1),
        20,
        Duration.ofMinutes(1),
        60,
        Duration.ofMinutes(1),
        10,
        Duration.ofMinutes(1),
        30,
        Duration.ofMinutes(1),
        10,
        Duration.ofMinutes(1),
        5,
        Duration.ofMinutes(5),
        10,
        Duration.ofMinutes(1));
  }
}
