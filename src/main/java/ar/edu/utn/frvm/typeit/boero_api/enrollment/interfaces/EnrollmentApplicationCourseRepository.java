package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentApplicationCourseRepository
    extends JpaRepository<EnrollmentApplicationCourse, UUID> {

  @EntityGraph(attributePaths = {"course", "course.studyPlanSpace", "preferredTeacher"})
  List<EnrollmentApplicationCourse> findByEnrollmentApplication_Id(UUID applicationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"enrollmentApplication", "course", "preferredTeacher"})
  @Query(
      "SELECT selection FROM EnrollmentApplicationCourse selection WHERE selection.id = :id AND selection.institution.id = :institutionId")
  Optional<EnrollmentApplicationCourse> findByIdAndInstitutionIdForUpdate(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Query(
      "SELECT selection FROM EnrollmentApplicationCourse selection WHERE selection.enrollmentApplication.id = :applicationId AND selection.status IN :statuses ORDER BY selection.requestedAt, selection.id")
  List<EnrollmentApplicationCourse> findByApplicationIdAndStatuses(
      @Param("applicationId") UUID applicationId,
      @Param("statuses") List<EnrollmentApplicationCourseStatus> statuses);

  @EntityGraph(attributePaths = {"enrollmentApplication"})
  @Query(
      "SELECT selection FROM EnrollmentApplicationCourse selection WHERE selection.institution.id = :institutionId AND selection.course.id = :courseId")
  List<EnrollmentApplicationCourse> findByCourseIdAndInstitutionId(
      @Param("courseId") UUID courseId, @Param("institutionId") UUID institutionId);

  @EntityGraph(
      attributePaths = {
        "enrollmentApplication",
        "enrollmentApplication.applicantPerson",
        "enrollmentApplication.preference",
        "course",
        "preferredTeacher"
      })
  @Query(
      "SELECT selection FROM EnrollmentApplicationCourse selection WHERE selection.institution.id = :institutionId AND selection.course.id = :courseId AND selection.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus.WAITLISTED ORDER BY selection.waitlistNumber")
  List<EnrollmentApplicationCourse> findWaitlistedByCourseIdAndInstitutionIdOrderByWaitlistNumber(
      @Param("courseId") UUID courseId, @Param("institutionId") UUID institutionId);

  @Query(
      "SELECT COUNT(selection) > 0 FROM EnrollmentApplicationCourse selection WHERE selection.institution.id = :institutionId AND selection.course.id = :courseId AND selection.status IN :statuses")
  boolean existsBlockingCourseSelection(
      @Param("institutionId") UUID institutionId,
      @Param("courseId") UUID courseId,
      @Param("statuses") List<EnrollmentApplicationCourseStatus> statuses);

  @Query(
      "SELECT COUNT(selection) > 0 FROM EnrollmentApplicationCourse selection WHERE selection.institution.id = :institutionId AND selection.course.id = :courseId AND selection.enrollmentApplication.id <> :applicationId AND selection.enrollmentApplication.applicantPerson.id = :personId AND selection.enrollmentApplication.deletedAt IS NULL AND selection.enrollmentApplication.status NOT IN (ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.CANCELLED, ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.REJECTED) AND selection.status IN :statuses")
  boolean existsBlockingCourseSelectionExcludingApplication(
      @Param("institutionId") UUID institutionId,
      @Param("courseId") UUID courseId,
      @Param("applicationId") UUID applicationId,
      @Param("personId") UUID personId,
      @Param("statuses") List<EnrollmentApplicationCourseStatus> statuses);

  @Query(
      "SELECT selection FROM EnrollmentApplicationCourse selection WHERE selection.institution.id = :institutionId AND selection.course.id = :courseId AND selection.status = :status AND selection.enrollmentApplication.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.APPROVED ORDER BY selection.requestedAt, selection.id")
  List<EnrollmentApplicationCourse> findApprovedPendingByCourse(
      @Param("institutionId") UUID institutionId,
      @Param("courseId") UUID courseId,
      @Param("status") EnrollmentApplicationCourseStatus status);
}
