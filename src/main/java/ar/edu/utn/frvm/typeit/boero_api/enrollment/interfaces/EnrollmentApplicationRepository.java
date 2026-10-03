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

  /**
   * Applications a person may act on: their own, or the ones of a dependent they have an active
   * guardianship link with. Any tutor of the applicant qualifies, not only whoever submitted it,
   * and the access ends as soon as the link is no longer active. Expects the {@code application}
   * alias and a {@code :personId} parameter.
   */
  String ACCESSIBLE_BY_PERSON =
      "(application.applicantPerson.id = :personId "
          + "OR EXISTS (SELECT 1 FROM PersonGuardian guardianship "
          + "WHERE guardianship.institution.id = application.institution.id "
          + "AND guardianship.tutorPerson.id = :personId "
          + "AND guardianship.status = "
          + "ar.edu.utn.frvm.typeit.boero_api.institutional.entities.GuardianLinkStatus.ACTIVE "
          + "AND guardianship.dependentPerson.id = application.applicantPerson.id))";

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.id = :id AND application.deletedAt IS NULL AND "
          + ACCESSIBLE_BY_PERSON)
  Optional<EnrollmentApplication> findAccessibleForUpdate(
      @Param("id") UUID id, @Param("personId") UUID personId);

  @Query(
      "SELECT COUNT(application) > 0 FROM EnrollmentApplication application "
          + "WHERE application.id = :applicationId AND application.deletedAt IS NULL AND "
          + ACCESSIBLE_BY_PERSON)
  boolean isAccessibleByPerson(
      @Param("applicationId") UUID applicationId, @Param("personId") UUID personId);

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
          + "AND application.deletedAt IS NULL "
          + "AND (:status IS NULL OR application.status = :status) "
          + "AND (:trainingPathId IS NULL OR application.studyPlan.trainingPath.id = :trainingPathId) "
          + "AND (:open = false OR application.enrollmentPeriod.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus.OPEN)")
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
        "academicYear",
      })
  @Query(
      "SELECT application FROM EnrollmentApplication application "
          + "WHERE application.deletedAt IS NULL "
          + "AND (:institutionId IS NULL OR application.institution.id = :institutionId) "
          + "AND (:status IS NULL OR application.status = :status) "
          + "AND (:trainingPathId IS NULL OR application.studyPlan.trainingPath.id = :trainingPathId) "
          + "AND (:open = false OR application.enrollmentPeriod.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus.OPEN)")
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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
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
  Optional<EnrollmentApplication> findByIdAndInstitutionIdForUpdate(
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
          + "AND "
          + ACCESSIBLE_BY_PERSON
          + " "
          + "AND application.deletedAt IS NULL "
          + "AND (:dependentPersonId IS NULL OR application.applicantPerson.id = :dependentPersonId) "
          + "AND (:status IS NULL OR application.status = :status)")
  Page<EnrollmentApplication> findMyApplications(
      @Param("institutionId") UUID institutionId,
      @Param("personId") UUID personId,
      @Param("dependentPersonId") @Nullable UUID dependentPersonId,
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
          + "AND "
          + ACCESSIBLE_BY_PERSON
          + " "
          + "AND application.id = :applicationId "
          + "AND application.deletedAt IS NULL")
  Optional<EnrollmentApplication> findAccessibleByIdAndInstitutionId(
      @Param("institutionId") UUID institutionId,
      @Param("personId") UUID personId,
      @Param("applicationId") UUID applicationId);
}
