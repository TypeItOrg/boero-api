package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"student", "student.person", "course", "courseClass"})
  @Query(
      "SELECT enrollment FROM CourseEnrollment enrollment WHERE enrollment.id = :id AND enrollment.institution.id = :institutionId")
  Optional<CourseEnrollment> findByIdAndInstitutionIdForUpdate(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @EntityGraph(attributePaths = {"student", "student.person", "course", "courseClass"})
  @Query(
      "SELECT enrollment FROM CourseEnrollment enrollment WHERE enrollment.id = :id AND enrollment.institution.id = :institutionId")
  Optional<CourseEnrollment> findByIdAndInstitutionId(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @EntityGraph(attributePaths = {"student", "student.person", "course", "courseClass"})
  Page<CourseEnrollment> findByInstitution_IdAndStudent_Id(
      UUID institutionId, UUID studentId, Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "student",
        "student.person",
        "course",
        "course.studyPlanSpace.academicSpace",
        "course.studyPlanSpace.academicLevel",
        "course.studyPlanSpace.studyPlan.trainingPath",
        "course.instrument",
        "courseClass"
      })
  @Query(
      "SELECT enrollment FROM CourseEnrollment enrollment WHERE enrollment.institution.id = :institutionId AND enrollment.student.person.id = :personId"
          + " AND (:status IS NULL OR enrollment.status = :status)"
          + " AND (:academicStatus IS NULL OR enrollment.academicStatus = :academicStatus)")
  Page<CourseEnrollment> findByInstitutionIdAndStudentPersonId(
      @Param("institutionId") UUID institutionId,
      @Param("personId") UUID personId,
      @Param("status") @Nullable CourseEnrollmentStatus status,
      @Param("academicStatus") @Nullable AcademicEnrollmentStatus academicStatus,
      Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "student",
        "student.person",
        "course",
        "course.studyPlanSpace.academicSpace",
        "course.studyPlanSpace.academicLevel",
        "course.studyPlanSpace.studyPlan.trainingPath",
        "course.instrument",
        "courseClass"
      })
  @Query(
      "SELECT enrollment FROM CourseEnrollment enrollment WHERE enrollment.institution.id = :institutionId"
          + " AND (:status IS NULL OR enrollment.status = :status)"
          + " AND (:academicStatus IS NULL OR enrollment.academicStatus = :academicStatus)")
  Page<CourseEnrollment> findByInstitution_Id(
      UUID institutionId,
      @Param("status") @Nullable CourseEnrollmentStatus status,
      @Param("academicStatus") @Nullable AcademicEnrollmentStatus academicStatus,
      Pageable pageable);

  @Query(
      "SELECT enrollment FROM CourseEnrollment enrollment WHERE enrollment.institution.id = :institutionId AND enrollment.student.id = :studentId AND enrollment.course.id = :courseId AND enrollment.status = :status")
  Optional<CourseEnrollment> findByStudentAndCourseAndStatus(
      @Param("institutionId") UUID institutionId,
      @Param("studentId") UUID studentId,
      @Param("courseId") UUID courseId,
      @Param("status") CourseEnrollmentStatus status);

  @Query(
      "SELECT enrollment FROM CourseEnrollment enrollment WHERE enrollment.institution.id = :institutionId AND enrollment.enrollmentApplication.id = :applicationId AND enrollment.course.id = :courseId")
  Optional<CourseEnrollment> findByApplicationIdAndCourseId(
      @Param("institutionId") UUID institutionId,
      @Param("applicationId") UUID applicationId,
      @Param("courseId") UUID courseId);

  @Query(
      "SELECT enrollment FROM CourseEnrollment enrollment WHERE enrollment.institution.id = :institutionId AND enrollment.course.id = :courseId AND enrollment.status = :status")
  List<CourseEnrollment> findByCourseAndStatus(
      @Param("institutionId") UUID institutionId,
      @Param("courseId") UUID courseId,
      @Param("status") CourseEnrollmentStatus status);

  @Query(
      "SELECT COUNT(enrollment) > 0 FROM CourseEnrollment enrollment WHERE enrollment.institution.id = :institutionId AND enrollment.course.id = :courseId")
  boolean existsByCourseIncludingHistorical(
      @Param("institutionId") UUID institutionId, @Param("courseId") UUID courseId);
}
