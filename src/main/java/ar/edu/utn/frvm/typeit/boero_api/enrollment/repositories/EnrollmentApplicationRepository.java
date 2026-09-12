package ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
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
          + "AND (:status IS NULL OR application.status = :status)")
  Page<EnrollmentApplication> findByInstitutionId(
      @Param("institutionId") UUID institutionId,
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
}
