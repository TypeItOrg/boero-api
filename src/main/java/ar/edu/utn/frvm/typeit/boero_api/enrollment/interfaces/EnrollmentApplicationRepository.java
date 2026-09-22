package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentApplicationRepository
    extends JpaRepository<EnrollmentApplication, UUID>,
        JpaSpecificationExecutor<EnrollmentApplication> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  java.util.Optional<EnrollmentApplication>
      findByInstitution_IdAndApplicantPerson_IdAndTrainingPathIdAndEnrollmentPeriod_IdAndStatusAndDeletedAtIsNull(
          UUID institutionId,
          UUID personId,
          UUID trainingPathId,
          UUID periodId,
          EnrollmentApplicationStatus status);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM enrollment_applications application
            WHERE application.status = 'SUBMITTED' AND application.deleted_at IS NULL
              AND (application.enrollment_period_id = :periodId OR EXISTS (
                SELECT 1 FROM enrollment_application_courses selection
                WHERE selection.enrollment_application_id = application.enrollment_application_id
                  AND selection.enrollment_period_id = :periodId)))
          """,
      nativeQuery = true)
  boolean existsSubmittedByPeriod(@Param("periodId") UUID periodId);

  @Query(
      "SELECT app FROM EnrollmentApplication app WHERE app.institution.id = :institutionId AND app.applicantPerson.id = :personId AND app.trainingPathId = :trainingPathId AND app.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.DRAFT AND app.deletedAt IS NULL ORDER BY CASE WHEN app.enrollmentPeriod IS NULL THEN 0 ELSE 1 END, app.updatedAt DESC, app.id DESC")
  List<EnrollmentApplication> findDraftsForTrainingPath(
      UUID institutionId, UUID personId, UUID trainingPathId, Pageable pageable);

  @Query(
      "SELECT app FROM EnrollmentApplication app WHERE app.id = :id AND app.applicantPerson.id = :personId AND app.deletedAt IS NULL")
  Optional<EnrollmentApplication> findOwnedForUpdate(
      @Param("id") UUID id, @Param("personId") UUID personId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT app FROM EnrollmentApplication app WHERE app.id = :id AND app.deletedAt IS NULL AND (:institutionId IS NULL OR app.institution.id = :institutionId)")
  Optional<EnrollmentApplication> findForAttachmentUpdate(
      @Param("id") UUID id, @Param("institutionId") @Nullable UUID institutionId);

  Optional<EnrollmentApplication>
      findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
          UUID applicantPersonId,
          UUID studyPlanId,
          UUID academicYearId,
          EnrollmentApplicationStatus status);

  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.applicantPerson.id = :personId "
          + "AND application.studyPlan.trainingPath.id = :trainingPathId "
          + "AND application.deletedAt IS NULL "
          + "AND application.status NOT IN ("
          + "ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.CANCELLED, "
          + "ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.REJECTED)")
  List<EnrollmentApplication> findActiveByApplicantPersonIdAndTrainingPathId(
      @Param("personId") UUID personId, @Param("trainingPathId") UUID trainingPathId);

  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.applicantPerson.id = :personId "
          + "AND application.institution.id = :institutionId "
          + "AND application.trainingPathId = :trainingPathId "
          + "AND application.academicYear.id = :academicYearId "
          + "AND application.deletedAt IS NULL "
          + "AND application.status IN ("
          + "ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.DRAFT, "
          + "ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.SUBMITTED)")
  List<EnrollmentApplication> findOpenByApplicantAndContext(
      @Param("personId") UUID personId,
      @Param("institutionId") UUID institutionId,
      @Param("trainingPathId") UUID trainingPathId,
      @Param("academicYearId") UUID academicYearId);

  @EntityGraph(
      attributePaths = {
        "institution",
        "applicantPerson",
        "studyPlan",
        "trainingPath",
        "academicYear",
      })
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.institution.id = :institutionId "
          + "AND application.deletedAt IS NULL "
          + "AND (:#{@scopedAuthorization.unrestricted('ENROLLMENT_APPLICATION_READ')} = true OR application.trainingPathId IN :#{@scopedAuthorization.paths('ENROLLMENT_APPLICATION_READ')}) AND (:status IS NULL OR application.status = :status) "
          + "AND (:trainingPathId IS NULL OR application.trainingPathId = :trainingPathId) "
          + "AND (:open = false OR EXISTS ("
          + "SELECT period.id FROM EnrollmentPeriod period "
          + "WHERE period.id = application.enrollmentPeriod.id "
          + "AND period.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus.OPEN) "
          + "OR EXISTS (SELECT selection.id FROM EnrollmentApplicationCourse selection "
          + "WHERE selection.enrollmentApplication.id = application.id "
          + "AND selection.enrollmentPeriod.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus.OPEN))")
  Page<EnrollmentApplication> findByInstitutionId(
      @Param("institutionId") UUID institutionId,
      @Param("status") @Nullable EnrollmentApplicationStatus status,
      @Param("trainingPathId") @Nullable UUID trainingPathId,
      @Param("open") boolean open,
      Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "institution",
        "applicantPerson",
        "studyPlan",
        "trainingPath",
        "academicYear",
      })
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.deletedAt IS NULL "
          + "AND (:institutionId IS NULL OR application.institution.id = :institutionId) "
          + "AND (:status IS NULL OR application.status = :status) "
          + "AND (:trainingPathId IS NULL OR application.trainingPathId = :trainingPathId) "
          + "AND (:open = false OR EXISTS ("
          + "SELECT period.id FROM EnrollmentPeriod period "
          + "WHERE period.id = application.enrollmentPeriod.id "
          + "AND period.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus.OPEN) "
          + "OR EXISTS (SELECT selection.id FROM EnrollmentApplicationCourse selection "
          + "WHERE selection.enrollmentApplication.id = application.id "
          + "AND selection.enrollmentPeriod.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus.OPEN))")
  Page<EnrollmentApplication> findByFilters(
      @Param("institutionId") @Nullable UUID institutionId,
      @Param("status") @Nullable EnrollmentApplicationStatus status,
      @Param("trainingPathId") @Nullable UUID trainingPathId,
      @Param("open") boolean open,
      Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "institution",
        "applicantPerson",
        "studyPlan",
        "academicYear",
      })
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.institution.id = :institutionId "
          + "AND application.id = :applicationId "
          + "AND application.deletedAt IS NULL")
  Optional<EnrollmentApplication> findByIdAndInstitutionId(
      @Param("institutionId") UUID institutionId, @Param("applicationId") UUID applicationId);

  // NOTE: no @EntityGraph here. Combining PESSIMISTIC_WRITE with a fetch graph
  // makes Hibernate 7 fail with UnknownTableReference for the @OneToOne(mappedBy)
  // associations (educationBackground, healthInclusion, ...). Associations load
  // lazily inside the caller's transaction instead.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.institution.id = :institutionId "
          + "AND application.id = :applicationId "
          + "AND application.deletedAt IS NULL")
  Optional<EnrollmentApplication> findByIdAndInstitutionIdForUpdate(
      @Param("institutionId") UUID institutionId, @Param("applicationId") UUID applicationId);

  @Query(
      "SELECT application.status FROM EnrollmentApplication application WHERE application.institution.id = :institutionId AND application.id = :applicationId AND application.deletedAt IS NULL")
  Optional<EnrollmentApplicationStatus> findStatusByInstitutionIdAndId(
      @Param("institutionId") UUID institutionId, @Param("applicationId") UUID applicationId);

  @EntityGraph(
      attributePaths = {
        "institution",
        "applicantPerson",
        "studyPlan",
        "academicYear",
      })
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.institution.id = :institutionId "
          + "AND application.applicantPerson.id = :personId "
          + "AND application.deletedAt IS NULL "
          + "AND (:status IS NULL OR application.status = :status)")
  Page<EnrollmentApplication> findMyApplications(
      @Param("institutionId") UUID institutionId,
      @Param("personId") UUID personId,
      @Param("status") @Nullable EnrollmentApplicationStatus status,
      Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "institution",
        "applicantPerson",
        "studyPlan",
        "academicYear",
      })
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.institution.id = :institutionId "
          + "AND application.applicantPerson.id = :personId "
          + "AND application.id = :applicationId "
          + "AND application.deletedAt IS NULL")
  Optional<EnrollmentApplication> findByIdAndApplicantPersonIdAndInstitutionId(
      @Param("institutionId") UUID institutionId,
      @Param("personId") UUID personId,
      @Param("applicationId") UUID applicationId);

  @Query(
      "SELECT application.institution.id FROM EnrollmentApplication application WHERE application.id = :applicationId AND application.applicantPerson.id = :personId AND application.deletedAt IS NULL")
  Optional<UUID> findOwnedInstitutionId(
      @Param("applicationId") UUID applicationId, @Param("personId") UUID personId);
}
