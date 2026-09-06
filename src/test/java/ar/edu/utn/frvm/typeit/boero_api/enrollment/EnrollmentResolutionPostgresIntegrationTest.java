package ar.edu.utn.frvm.typeit.boero_api.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentApplicationStateException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.MissingRejectionReasonException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.RejectEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ApproveEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.RejectEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.StudentRepository;
import ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData;
import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest(
    properties = {
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/migration",
      "spring.flyway.sql-migration-prefix=",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@IntegrationTest
@Import({
  JpaAuditingTestConfig.class,
  ApproveEnrollmentApplicationUseCase.class,
  RejectEnrollmentApplicationUseCase.class
})
class EnrollmentResolutionPostgresIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

  @Autowired private EntityManager entityManager;
  @Autowired private EnrollmentApplicationRepository enrollmentApplicationRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private ApproveEnrollmentApplicationUseCase approveUseCase;
  @Autowired private RejectEnrollmentApplicationUseCase rejectUseCase;

  private Institution institution;
  private Person person;
  private UUID studyPlanId;
  private UUID academicYearId;
  private EnrollmentPeriod enrollmentPeriod;

  @DynamicPropertySource
  static void databaseProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @BeforeEach
  void seedFixture() {
    institution = InstitutionalTestData.createInstitution(entityManager, "cons-enrollment");
    person = InstitutionalTestData.person(institution, "30000001");
    InstitutionalTestData.persist(entityManager, person);

    final var trainingPath =
        TrainingPath.create(institution, "Formación Básica", "Programa de ingreso");
    InstitutionalTestData.persist(entityManager, trainingPath);
    final var studyPlan =
        StudyPlan.create(
            institution,
            trainingPath,
            "Profesorado de Música",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2030, 12, 31));
    InstitutionalTestData.persist(entityManager, studyPlan);
    studyPlanId = studyPlan.getId();

    final int year = Year.now().getValue() + 1;
    final var academicYear =
        AcademicYear.create(
            institution, year, LocalDate.of(year, 2, 1), LocalDate.of(year, 12, 15));
    InstitutionalTestData.persist(entityManager, academicYear);
    academicYearId = academicYear.getId();

    enrollmentPeriod =
        EnrollmentPeriod.builder()
            .institution(institution)
            .academicYear(academicYear)
            .name("Inscripción " + year)
            .startDate(LocalDateTime.of(year, 2, 1, 0, 0))
            .endDate(LocalDateTime.of(year, 12, 15, 23, 59))
            .status(EnrollmentPeriodStatus.OPEN)
            .build();
    InstitutionalTestData.persist(entityManager, enrollmentPeriod);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName(
      "Should approve a submitted application, create the student and block a second approval")
  void approve_createsStudentAndCannotResolveTwice() {
    final var applicationId = startAndSubmit();

    final var approved = approveUseCase.execute(institution.getId(), applicationId);

    assertThat(approved.status()).isEqualTo(EnrollmentApplicationStatus.APPROVED);
    entityManager.flush();
    entityManager.clear();
    assertThat(
            studentRepository.existsByInstitution_IdAndPerson_Id(
                institution.getId(), person.getId()))
        .isTrue();
    assertThat(studentRepository.countByInstitution_Id(institution.getId())).isEqualTo(1);

    assertThatThrownBy(() -> approveUseCase.execute(institution.getId(), applicationId))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);

    final var reloaded = enrollmentApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(EnrollmentApplicationStatus.APPROVED);
    assertThat(reloaded.getResolvedAt()).isNotNull();
    assertThat(studentRepository.countByInstitution_Id(institution.getId())).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "Should reject a submitted application storing the reason and block a second resolution")
  void reject_storesReasonAndCannotResolveTwice() {
    final var applicationId = startAndSubmit();

    final var rejected =
        rejectUseCase.execute(
            institution.getId(),
            applicationId,
            new RejectEnrollmentApplicationRequest("Documentación incompleta"));

    assertThat(rejected.status()).isEqualTo(EnrollmentApplicationStatus.REJECTED);
    assertThat(rejected.rejectionReason()).isEqualTo("Documentación incompleta");
    assertThat(rejected.resolvedAt()).isNotNull();

    assertThatThrownBy(
            () ->
                rejectUseCase.execute(
                    institution.getId(),
                    applicationId,
                    new RejectEnrollmentApplicationRequest("Otro motivo")))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);

    final var reloaded = enrollmentApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(EnrollmentApplicationStatus.REJECTED);
    assertThat(reloaded.getRejectionReason()).isEqualTo("Documentación incompleta");
    assertThat(reloaded.getResolvedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should enforce non-blank rejection reason constraint through JPA pre-check")
  void reject_rejectsBlankReason() {
    final var applicationId = startAndSubmit();

    assertThatThrownBy(
            () ->
                rejectUseCase.execute(
                    institution.getId(),
                    applicationId,
                    new RejectEnrollmentApplicationRequest(" ")))
        .isInstanceOf(MissingRejectionReasonException.class);
  }

  @Test
  @DisplayName(
      "Should enforce PostgreSQL consistency check: rejection_reason cannot be set when status is not REJECTED")
  void database_enforcesResolutionConsistencyCheck_cannotHaveRejectionReasonWhenNotRejected() {
    final var applicationId = startAndSubmit();

    assertThatThrownBy(
            () ->
                entityManager
                    .createNativeQuery(
                        """
                        INSERT INTO enrollment_applications (
                          enrollment_application_id, institution_id, applicant_person_id,
                          study_plan_id, academic_year_id, enrollment_period_id, status,
                          rejection_reason, created_at, updated_at
                        ) VALUES (:id, :institutionId, :personId, :studyPlanId, :academicYearId,
                                 :periodId, 'SUBMITTED', 'Reason without rejected status', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("institutionId", institution.getId())
                    .setParameter("personId", person.getId())
                    .setParameter("studyPlanId", studyPlanId)
                    .setParameter("academicYearId", academicYearId)
                    .setParameter(
                        "periodId",
                        enrollmentApplicationRepository
                            .findById(applicationId)
                            .orElseThrow()
                            .getEnrollmentPeriod()
                            .getId())
                    .executeUpdate())
        .isInstanceOf(PersistenceException.class)
        .hasMessageContaining("enrollment_applications_resolution_consistency_check");
  }

  @Test
  @DisplayName(
      "Should enforce PostgreSQL consistency check: rejection_reason must be set when status is REJECTED")
  void database_enforcesResolutionConsistencyCheck_cannotHaveNullRejectionReasonWhenRejected() {
    final var applicationId = startAndSubmit();

    assertThatThrownBy(
            () ->
                entityManager
                    .createNativeQuery(
                        """
                        INSERT INTO enrollment_applications (
                          enrollment_application_id, institution_id, applicant_person_id,
                          study_plan_id, academic_year_id, enrollment_period_id, status,
                          rejection_reason, created_at, updated_at
                        ) VALUES (:id, :institutionId, :personId, :studyPlanId, :academicYearId,
                                 :periodId, 'REJECTED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("institutionId", institution.getId())
                    .setParameter("personId", person.getId())
                    .setParameter("studyPlanId", studyPlanId)
                    .setParameter("academicYearId", academicYearId)
                    .setParameter(
                        "periodId",
                        enrollmentApplicationRepository
                            .findById(applicationId)
                            .orElseThrow()
                            .getEnrollmentPeriod()
                            .getId())
                    .executeUpdate())
        .isInstanceOf(PersistenceException.class)
        .hasMessageContaining("enrollment_applications_resolution_consistency_check");
  }

  private UUID startAndSubmit() {
    final var application =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(entityManager.find(StudyPlan.class, studyPlanId))
            .academicYear(entityManager.find(AcademicYear.class, academicYearId))
            .enrollmentPeriod(enrollmentPeriod)
            .status(EnrollmentApplicationStatus.SUBMITTED)
            .build();
    enrollmentApplicationRepository.saveAndFlush(application);
    return application.getId();
  }
}
