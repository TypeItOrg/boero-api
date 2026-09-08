package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.ResponsibleDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData;
import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Ejercita EnrollmentApplicationService contra un Postgres real migrado con Flyway (ddl-auto
 * validate), no contra repositorios mockeados: es la única forma de detectar tanto una violación
 * real de NOT NULL en el autoguardado parcial como el filtro real de institución en el listado
 * (mockear la Specification con any() nunca ejecuta el predicate).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@IntegrationTest
class EnrollmentApplicationPostgresIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Autowired private EnrollmentApplicationService service;
  @Autowired private EntityManager entityManager;

  @DynamicPropertySource
  static void infrastructureProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("spring.flyway.enabled", () -> true);
    registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/dev");
    registry.add("spring.flyway.sql-migration-prefix", () -> "");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Test
  @Transactional
  @DisplayName(
      "Should autosave a partial responsible section without violating NOT NULL constraints")
  void updateDraft_partialResponsible_doesNotViolateConstraints() {
    EnrollmentApplication application = createDraftApplication();

    UpdateEnrollmentDraftRequest request =
        UpdateEnrollmentDraftRequest.builder()
            .data(
                EnrollmentDraftData.builder()
                    .responsible(ResponsibleDto.builder().fullName("Tutor Incompleto").build())
                    .build())
            .build();

    EnrollmentApplicationResponse response =
        service.updateDraft(
            application.getApplicantPerson().getId(), application.getId(), request);
    entityManager.flush();

    assertThat(response.getData().getResponsible().getFullName()).isEqualTo("Tutor Incompleto");
    assertThat(response.getData().getResponsible().getDocumentNumber()).isNull();
  }

  @Test
  @Transactional
  @DisplayName("Should isolate the application listing by institution using the real Specification")
  void listApplications_isolatesByRealInstitutionFilter() {
    EnrollmentApplication applicationA = createDraftApplication();
    EnrollmentApplication applicationB = createDraftApplication();

    PaginatedResponse<EnrollmentApplicationResponse> response =
        service.listApplications(
            applicationA.getInstitution().getId(), null, null, null, PageRequest.of(0, 10));

    assertThat(response.items())
        .extracting(EnrollmentApplicationResponse::getApplicationId)
        .containsExactly(applicationA.getId())
        .doesNotContain(applicationB.getId());
  }

  private EnrollmentApplication createDraftApplication() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Institution institution =
        InstitutionalTestData.createInstitution(entityManager, "enrollment-" + suffix);
    Person person =
        InstitutionalTestData.persist(
            entityManager, InstitutionalTestData.person(institution, docNumber()));

    TrainingPath trainingPath =
        InstitutionalTestData.persist(
            entityManager, TrainingPath.create(institution, "Instrumento " + suffix, null));
    StudyPlan studyPlan =
        InstitutionalTestData.persist(
            entityManager,
            StudyPlan.create(institution, trainingPath, "Piano " + suffix, LocalDate.now(), null));
    AcademicYear academicYear =
        InstitutionalTestData.persist(
            entityManager, AcademicYear.create(institution, 2026, null, null));

    EnrollmentPeriod period =
        InstitutionalTestData.persist(
            entityManager,
            EnrollmentPeriod.builder()
                .institution(institution)
                .academicYear(academicYear)
                .name("Periodo " + suffix)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status(EnrollmentPeriodStatus.OPEN)
                .build());

    EnrollmentApplication application =
        InstitutionalTestData.persist(
            entityManager,
            EnrollmentApplication.builder()
                .institution(institution)
                .applicantPerson(person)
                .studyPlan(studyPlan)
                .academicYear(academicYear)
                .enrollmentPeriod(period)
                .status(EnrollmentApplicationStatus.DRAFT)
                .build());

    entityManager.flush();
    return application;
  }

  private String docNumber() {
    return String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
  }
}
