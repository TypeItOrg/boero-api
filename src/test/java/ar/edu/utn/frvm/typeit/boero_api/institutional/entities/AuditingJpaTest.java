package ar.edu.utn.frvm.typeit.boero_api.institutional.entities;

import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.city;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.country;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.institution;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.persist;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.person;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.province;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.user;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingTestConfig.class)
class AuditingJpaTest {

  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("Should audit and persist institutional user mapping")
  void shouldAuditAndPersistInstitutionalUserMapping() {
    Country country = persist(entityManager, country("ARG"));
    Province province = persist(entityManager, province(country, "14"));
    City city = persist(entityManager, city(province, "14014010"));
    Institution institution = persist(entityManager, institution(city, "boero"));
    Person person = persist(entityManager, person(institution, "12345678"));
    User user = persist(entityManager, user(institution, person));

    entityManager.flush();

    assertThat(user.getId()).isNotNull();
    assertThat(user.getId().version()).isEqualTo(7);
    assertThat(user.getInstitution().getId()).isEqualTo(institution.getId());
    assertThat(user.getPerson().getId()).isEqualTo(person.getId());
    assertThat(country.getId().version()).isEqualTo(7);
    assertThat(province.getId().version()).isEqualTo(7);
    assertThat(city.getId().version()).isEqualTo(7);
    assertThat(institution.getId().version()).isEqualTo(7);
    assertThat(person.getId().version()).isEqualTo(7);
    assertAudited(country);
    assertAudited(province);
    assertAudited(city);
    assertAudited(institution);
    assertAudited(person);
    assertAudited(user);
  }

  @Test
  @DisplayName("Should update last modified date when auditable entity changes")
  void shouldUpdateLastModifiedDateWhenAuditableEntityChanges() {
    Country country = persist(entityManager, country("URY"));
    entityManager.flush();
    LocalDateTime firstUpdatedAt = country.getUpdatedAt();

    country.setName("Republica Oriental del Uruguay");
    entityManager.flush();

    assertThat(country.getUpdatedAt()).isNotNull();
    assertThat(country.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
  }

  @Test
  @DisplayName("Should audit refresh token creation")
  void shouldAuditRefreshTokenCreation() {
    Institution institution = createInstitution("refresh-audit");
    User user = createUser(institution, "20000001");
    UserSession session = persist(entityManager, userSession(user));
    RefreshToken refreshToken =
        RefreshToken.builder()
            .sessionId(session.getId())
            .tokenHash("refresh-hash")
            .familyId(UUID.randomUUID().toString())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();

    persist(entityManager, refreshToken);
    entityManager.flush();

    assertThat(refreshToken.getId()).isNotNull();
    assertThat(refreshToken.getId().version()).isEqualTo(7);
    assertThat(refreshToken.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should set session start time when user session is created")
  void shouldSetSessionStartTimeWhenUserSessionIsCreated() {
    Institution institution = createInstitution("session-audit");
    User user = createUser(institution, "30000001");
    UserSession session = userSession(user);

    persist(entityManager, session);
    entityManager.flush();

    assertThat(session.getId()).isNotNull();
    assertThat(session.getId().version()).isEqualTo(7);
    assertThat(session.getStartedAt()).isNotNull();
  }

  private Institution createInstitution(final String slug) {
    return ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createInstitution(
        entityManager, slug);
  }

  private User createUser(final Institution institution, final String documentNumber) {
    return ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createUser(
        entityManager, institution, documentNumber);
  }

  private static UserSession userSession(final User user) {
    return UserSession.builder()
        .userId(user.getId())
        .ipAddress("192.0.2.10")
        .userAgent("Mozilla/5.0")
        .rememberMe(true)
        .build();
  }

  private static void assertAudited(
      ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable entity) {
    assertThat(entity.getCreatedAt()).isNotNull();
    assertThat(entity.getUpdatedAt()).isNotNull();
  }
}
