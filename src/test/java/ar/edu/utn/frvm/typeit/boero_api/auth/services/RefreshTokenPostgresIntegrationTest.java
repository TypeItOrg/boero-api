package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.TokenRefreshException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData;
import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@Import({RefreshTokenUseCase.class, JpaAuditingTestConfig.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@IntegrationTest
class RefreshTokenPostgresIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

  @Autowired private RefreshTokenUseCase useCase;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private UserSessionRepository userSessionRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;

  @MockitoBean private JwtService jwtService;
  @MockitoBean private JwtProperties jwtProperties;
  @MockitoBean private CacheManager cacheManager;

  @DynamicPropertySource
  static void postgresProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
  }

  @Test
  @DisplayName("Should commit family and session revocation after refresh token reuse")
  void execute_commitsRevocationAfterReuseException() {
    final Fixture fixture = createFixture(true);

    assertThatThrownBy(() -> useCase.execute(new RefreshTokenRequest(fixture.rawRefreshToken())))
        .isInstanceOf(TokenRefreshException.class);

    inTransaction(
        () -> {
          assertThat(refreshTokenRepository.findByFamilyId(fixture.familyId()))
              .allMatch(RefreshToken::isRevoked);
          assertThat(userSessionRepository.findById(fixture.sessionId()))
              .get()
              .returns(false, UserSession::isActive);
          return null;
        });
  }

  @Test
  @DisplayName("Should serialize concurrent rotation and invalidate the reused token family")
  void execute_serializesConcurrentRotation() throws Exception {
    final Fixture fixture = createFixture(false);
    when(jwtProperties.refreshExpiration(false)).thenReturn(Duration.ofDays(7));
    when(jwtService.generateAccessToken(any(InstitutionalAccessTokenInput.class)))
        .thenReturn("access-token");
    final CountDownLatch ready = new CountDownLatch(2);
    final CountDownLatch start = new CountDownLatch(1);
    final Callable<Boolean> rotation =
        () -> {
          ready.countDown();
          start.await();
          try {
            useCase.execute(new RefreshTokenRequest(fixture.rawRefreshToken()));
            return true;
          } catch (TokenRefreshException exception) {
            return false;
          }
        };

    try (var executor = Executors.newFixedThreadPool(2)) {
      final var first = executor.submit(rotation);
      final var second = executor.submit(rotation);
      ready.await();
      start.countDown();

      assertThat(java.util.List.of(first.get(), second.get()))
          .containsExactlyInAnyOrder(true, false);
    }

    inTransaction(
        () -> {
          assertThat(refreshTokenRepository.findByFamilyId(fixture.familyId()))
              .allMatch(RefreshToken::isRevoked);
          assertThat(userSessionRepository.findById(fixture.sessionId()))
              .get()
              .returns(false, UserSession::isActive);
          return null;
        });
  }

  private Fixture createFixture(final boolean revoked) {
    return inTransaction(
        () -> {
          final String suffix = UUID.randomUUID().toString().substring(0, 8);
          final Institution institution =
              InstitutionalTestData.createInstitution(entityManager, "refresh-" + suffix);
          final String documentNumber =
              String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
          final User user =
              InstitutionalTestData.createUser(entityManager, institution, documentNumber);
          final UserSession session =
              UserSession.builder()
                  .userId(user.getId())
                  .ipAddress("192.0.2.10")
                  .userAgent("PostgreSQL integration test")
                  .active(true)
                  .build();
          entityManager.persist(session);
          final String rawToken = "refresh-" + UUID.randomUUID();
          final String familyId = UUID.randomUUID().toString();
          final RefreshToken refreshToken =
              RefreshToken.builder()
                  .sessionId(session.getId())
                  .tokenHash(JwtService.hashToken(rawToken))
                  .familyId(familyId)
                  .expiresAt(LocalDateTime.now().plusDays(7))
                  .revoked(revoked)
                  .build();
          entityManager.persist(refreshToken);
          return new Fixture(rawToken, familyId, session.getId());
        });
  }

  private <T> T inTransaction(final Supplier<T> action) {
    return new TransactionTemplate(transactionManager).execute(status -> action.get());
  }

  private record Fixture(String rawRefreshToken, String familyId, UUID sessionId) {}
}
