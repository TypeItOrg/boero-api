package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
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

public interface CourseRepository extends JpaRepository<Course, UUID> {
  @EntityGraph(
      attributePaths = {
        "institution",
        "studyPlan",
        "studyPlan.trainingPath",
        "academicSpace",
        "academicYear"
      })
  @Query(
      """
      SELECT course FROM Course course
      WHERE (:institutionId IS NULL OR course.institution.id = :institutionId)
        AND ((:deleted = true AND course.deletedAt IS NOT NULL) OR (:deleted = false AND course.deletedAt IS NULL))
        AND (:status IS NULL OR course.status = :status)
        AND (:academicSpaceId IS NULL OR course.academicSpace.id = :academicSpaceId)
        AND (:trainingPathId IS NULL OR course.studyPlan.trainingPath.id = :trainingPathId)
        AND (:studyPlanId IS NULL OR course.studyPlan.id = :studyPlanId)
        AND (:year IS NULL OR course.academicYear.year = :year)
        AND (:search IS NULL OR UNACCENT_LOWER(course.academicSpace.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(course.institution.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      """)
  Page<Course> findByFilters(
      @Param("institutionId") @Nullable UUID institutionId,
      @Param("search") @Nullable String search,
      @Param("status") @Nullable CourseStatus status,
      @Param("academicSpaceId") @Nullable UUID academicSpaceId,
      @Param("trainingPathId") @Nullable UUID trainingPathId,
      @Param("studyPlanId") @Nullable UUID studyPlanId,
      @Param("year") @Nullable Integer year,
      @Param("deleted") boolean deleted,
      Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "institution",
        "studyPlan",
        "studyPlan.trainingPath",
        "academicSpace",
        "academicYear"
      })
  @Query(
      "SELECT course FROM Course course WHERE course.id = :id AND course.institution.id = :institutionId AND course.deletedAt IS NULL")
  Optional<Course> findByIdAndInstitution_Id(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT course FROM Course course WHERE course.id = :id AND course.institution.id = :institutionId AND course.deletedAt IS NULL")
  Optional<Course> findByIdAndInstitution_IdForUpdate(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT course FROM Course course WHERE course.id = :id AND course.institution.id = :institutionId")
  Optional<Course> findByIdAndInstitution_IdForLifecycle(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Query(
      "SELECT course.academicYear.id FROM Course course WHERE course.id = :courseId AND course.institution.id = :institutionId")
  Optional<UUID> findAcademicYearIdByIdAndInstitution_Id(
      @Param("courseId") UUID courseId, @Param("institutionId") UUID institutionId);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM courses WHERE institution_id = :institutionId AND academic_space_id = :academicSpaceId AND academic_year_id = :academicYearId AND deleted_at IS NULL)",
      nativeQuery = true)
  boolean existsByInstitutionAndSpaceAndYear(
      @Param("institutionId") UUID institutionId,
      @Param("academicSpaceId") UUID academicSpaceId,
      @Param("academicYearId") UUID academicYearId);

  boolean existsByInstitution_IdAndStudyPlan_IdAndStatusAndDeletedAtIsNull(
      UUID institutionId, UUID studyPlanId, CourseStatus status);

  boolean existsByInstitution_IdAndStudyPlan_IdAndStatusNotAndDeletedAtIsNull(
      UUID institutionId, UUID studyPlanId, CourseStatus status);

  default boolean existsByInstitution_IdAndStudyPlan_IdAndActiveTrueAndDeletedAtIsNull(
      final UUID institutionId, final UUID studyPlanId) {
    return existsByInstitution_IdAndStudyPlan_IdAndStatusAndDeletedAtIsNull(
        institutionId, studyPlanId, CourseStatus.ACTIVE);
  }

  default boolean existsByInstitution_IdAndStudyPlan_IdAndStatusNotClosedAndDeletedAtIsNull(
      final UUID institutionId, final UUID studyPlanId) {
    return existsByInstitution_IdAndStudyPlan_IdAndStatusNotAndDeletedAtIsNull(
        institutionId, studyPlanId, CourseStatus.CLOSED);
  }

  List<Course> findByAcademicYear_IdAndInstitution_Id(UUID academicYearId, UUID institutionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<Course> findByAcademicYear_IdAndInstitution_IdAndDeletedAtIsNull(
      UUID academicYearId, UUID institutionId);

  List<Course> findByAcademicYear_Id(UUID academicYearId);

  @Query(
      "SELECT COUNT(c) FROM Course c WHERE c.institution.id = :institutionId AND c.academicYear.id = :academicYearId AND c.deletedAt IS NULL AND c.status != :excludedStatus")
  long countByInstitutionIdAndAcademicYearIdAndStatusNot(
      @Param("institutionId") UUID institutionId,
      @Param("academicYearId") UUID academicYearId,
      @Param("excludedStatus") CourseStatus excludedStatus);

  @Query("SELECT c FROM Course c WHERE c.academicYear.id = :academicYearId")
  List<Course> findAllByAcademicYearId(@Param("academicYearId") UUID academicYearId);
}
