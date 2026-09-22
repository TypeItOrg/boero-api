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
        "studyPlanSpace",
        "studyPlanSpace.studyPlan",
        "studyPlanSpace.studyPlan.trainingPath",
        "studyPlanSpace.academicSpace",
        "studyPlanSpace.academicLevel",
        "academicYear",
        "instrument"
      })
  @Query(
      """
      SELECT course FROM Course course
      WHERE (:#{@scopedAuthorization.unrestricted('COURSE_READ')} = true OR course.studyPlanSpace.studyPlan.trainingPath.id IN :#{@scopedAuthorization.paths('COURSE_READ')})
        AND (:institutionId IS NULL OR course.institution.id = :institutionId)
        AND ((:deleted = true AND course.deletedAt IS NOT NULL) OR (:deleted = false AND course.deletedAt IS NULL))
        AND (:status IS NULL OR course.status = :status)
        AND (:academicSpaceId IS NULL OR course.academicSpaceId = :academicSpaceId)
        AND (:trainingPathId IS NULL OR course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId)
        AND (:studyPlanId IS NULL OR course.studyPlanSpace.studyPlan.id = :studyPlanId)
        AND (:year IS NULL OR course.academicYear.year = :year)
        AND (:search IS NULL OR UNACCENT_LOWER(course.studyPlanSpace.academicSpace.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
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
        "studyPlanSpace",
        "studyPlanSpace.studyPlan",
        "studyPlanSpace.studyPlan.trainingPath",
        "studyPlanSpace.academicSpace",
        "studyPlanSpace.academicLevel",
        "academicYear",
        "instrument"
      })
  @Query(
      """
      SELECT course FROM Course course
      WHERE (:#{@scopedAuthorization.enrollmentOptionsUnrestricted()} = true OR course.studyPlanSpace.studyPlan.trainingPath.id IN :#{@scopedAuthorization.enrollmentOptionsPaths()})
        AND (:institutionId IS NULL OR course.institution.id = :institutionId)
        AND ((:deleted = true AND course.deletedAt IS NOT NULL) OR (:deleted = false AND course.deletedAt IS NULL))
        AND (:status IS NULL OR course.status = :status)
        AND (:academicSpaceId IS NULL OR course.academicSpaceId = :academicSpaceId)
        AND (:trainingPathId IS NULL OR course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId)
        AND (:studyPlanId IS NULL OR course.studyPlanSpace.studyPlan.id = :studyPlanId)
        AND (:year IS NULL OR course.academicYear.year = :year)
        AND (:search IS NULL OR UNACCENT_LOWER(course.studyPlanSpace.academicSpace.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(course.institution.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      """)
  Page<Course> findEnrollmentOptions(
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
        "studyPlanSpace",
        "studyPlanSpace.studyPlan",
        "studyPlanSpace.studyPlan.trainingPath",
        "studyPlanSpace.academicSpace",
        "academicYear",
        "instrument"
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
      """
      SELECT course.academicYear.id AS academicYearId, course.studyPlanSpace.studyPlan.id AS studyPlanId
      FROM Course course
      WHERE course.id = :courseId AND course.institution.id = :institutionId
      """)
  Optional<CourseAcademicContext> findAcademicContextByIdAndInstitution_Id(
      @Param("courseId") UUID courseId, @Param("institutionId") UUID institutionId);

  interface CourseAcademicContext {
    UUID getAcademicYearId();

    UUID getStudyPlanId();
  }

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM courses WHERE institution_id = :institutionId AND academic_space_id = :academicSpaceId AND academic_year_id = :academicYearId AND deleted_at IS NULL)",
      nativeQuery = true)
  boolean existsByInstitutionAndSpaceAndYear(
      @Param("institutionId") UUID institutionId,
      @Param("academicSpaceId") UUID academicSpaceId,
      @Param("academicYearId") UUID academicYearId);

  @Query(
      """
      SELECT COUNT(course) > 0 FROM Course course
      WHERE course.institution.id = :institutionId
        AND course.instrument.id = :instrumentId
        AND course.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.ACTIVE
        AND course.deletedAt IS NULL
      """)
  boolean existsActiveByInstrument(
      @Param("institutionId") UUID institutionId, @Param("instrumentId") UUID instrumentId);

  @Query(
      """
      SELECT COUNT(course) > 0 FROM Course course
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId
        AND course.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.ACTIVE
        AND course.deletedAt IS NULL
      """)
  boolean existsActiveByTrainingPath(
      @Param("institutionId") UUID institutionId, @Param("trainingPathId") UUID trainingPathId);

  @Query(
      "SELECT COUNT(course) > 0 FROM Course course WHERE course.studyPlanSpace.id = :studyPlanSpaceId")
  boolean existsByStudyPlanSpaceId(@Param("studyPlanSpaceId") UUID studyPlanSpaceId);

  @EntityGraph(
      attributePaths = {
        "studyPlanSpace",
        "studyPlanSpace.studyPlan",
        "studyPlanSpace.studyPlan.trainingPath",
        "studyPlanSpace.academicSpace",
        "studyPlanSpace.academicLevel",
        "academicYear",
        "instrument"
      })
  @Query(
      """
      SELECT course FROM Course course
      LEFT JOIN course.studyPlanSpace.academicLevel level
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId
        AND course.academicYear.id = :academicYearId
        AND course.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.ACTIVE
        AND course.deletedAt IS NULL
      ORDER BY level.displayOrder NULLS LAST,
               course.studyPlanSpace.academicSpace.name,
               course.id
      """)
  List<Course> findActiveByTrainingPathAndAcademicYear(
      @Param("institutionId") UUID institutionId,
      @Param("trainingPathId") UUID trainingPathId,
      @Param("academicYearId") UUID academicYearId);

  @EntityGraph(
      attributePaths = {
        "studyPlanSpace",
        "studyPlanSpace.studyPlan",
        "studyPlanSpace.studyPlan.trainingPath",
        "studyPlanSpace.academicSpace",
        "studyPlanSpace.academicLevel",
        "academicYear",
        "instrument"
      })
  // NOTE: the instrument search uses an explicit LEFT JOIN. Navigating
  // course.instrument.name directly would add an implicit INNER JOIN and silently
  // exclude every course without instrument, even when :search IS NULL.
  @Query(
      """
      SELECT course FROM Course course
      LEFT JOIN course.instrument instrument
      LEFT JOIN course.studyPlanSpace.academicLevel level
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId
        AND course.academicYear.id = :academicYearId
        AND course.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.ACTIVE
        AND course.deletedAt IS NULL
        AND (:search IS NULL OR UNACCENT_LOWER(course.studyPlanSpace.academicSpace.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(course.studyPlanSpace.studyPlan.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(COALESCE(instrument.name, '')) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      ORDER BY level.displayOrder NULLS LAST,
               course.studyPlanSpace.academicSpace.name,
               course.id
      """)
  Page<Course> findActiveByTrainingPathAndAcademicYear(
      @Param("institutionId") UUID institutionId,
      @Param("trainingPathId") UUID trainingPathId,
      @Param("academicYearId") UUID academicYearId,
      @Param("search") @Nullable String search,
      Pageable pageable);

  @Query(
      """
      SELECT COUNT(course) > 0 FROM Course course
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.id = :studyPlanId
        AND course.status = :status
        AND course.deletedAt IS NULL
      """)
  boolean existsByInstitution_IdAndStudyPlan_IdAndStatusAndDeletedAtIsNull(
      @Param("institutionId") UUID institutionId,
      @Param("studyPlanId") UUID studyPlanId,
      @Param("status") CourseStatus status);

  @Query(
      """
      SELECT COUNT(course) > 0 FROM Course course
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.id = :studyPlanId
        AND course.status <> :status
        AND course.deletedAt IS NULL
      """)
  boolean existsByInstitution_IdAndStudyPlan_IdAndStatusNotAndDeletedAtIsNull(
      @Param("institutionId") UUID institutionId,
      @Param("studyPlanId") UUID studyPlanId,
      @Param("status") CourseStatus status);

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
      "SELECT COUNT(c) FROM Course c WHERE c.institution.id = :institutionId AND c.academicYear.id = :academicYearId AND c.deletedAt IS NULL AND c.status != :excludedStatus AND (:#{@scopedAuthorization.unrestricted('COURSE_READ')} = true OR c.studyPlanSpace.studyPlan.trainingPath.id IN :#{@scopedAuthorization.paths('COURSE_READ')})")
  long countByInstitutionIdAndAcademicYearIdAndStatusNot(
      @Param("institutionId") UUID institutionId,
      @Param("academicYearId") UUID academicYearId,
      @Param("excludedStatus") CourseStatus excludedStatus);

  @Query("SELECT c FROM Course c WHERE c.academicYear.id = :academicYearId")
  List<Course> findAllByAcademicYearId(@Param("academicYearId") UUID academicYearId);

  @Query(
      """
      SELECT COUNT(course) > 0 FROM Course course
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId
        AND course.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.ACTIVE
        AND course.deletedAt IS NULL AND EXISTS (SELECT selection.id FROM EnrollmentPeriodOfferingLevel selection
          WHERE selection.offering.period.id = :periodId
            AND selection.offering.period.scopeConfigured = true
            AND selection.offering.studyPlan.deletedAt IS NULL
            AND selection.offering.studyPlan.trainingPath.deletedAt IS NULL
            AND selection.offering.studyPlan.trainingPath.active = true
            AND selection.offering.period.academicYear.id = course.academicYear.id
            AND selection.offering.studyPlan.id = course.studyPlanSpace.studyPlan.id
            AND (selection.academicLevel.id = course.studyPlanSpace.academicLevel.id
              OR selection.academicLevel IS NULL AND course.studyPlanSpace.academicLevel IS NULL))
      """)
  boolean existsOfferedForPeriod(UUID institutionId, UUID trainingPathId, UUID periodId);

  @EntityGraph(
      attributePaths = {
        "studyPlanSpace",
        "studyPlanSpace.studyPlan",
        "studyPlanSpace.studyPlan.trainingPath",
        "studyPlanSpace.academicSpace",
        "studyPlanSpace.academicLevel",
        "academicYear",
        "instrument"
      })
  @Query(
      """
      SELECT course FROM Course course
      LEFT JOIN course.instrument instrument
      LEFT JOIN course.studyPlanSpace.academicLevel level
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId
        AND course.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.ACTIVE
        AND course.deletedAt IS NULL AND EXISTS (SELECT selection.id FROM EnrollmentPeriodOfferingLevel selection
          WHERE selection.offering.period.id = :periodId
            AND selection.offering.period.scopeConfigured = true
            AND selection.offering.studyPlan.deletedAt IS NULL
            AND selection.offering.studyPlan.trainingPath.deletedAt IS NULL
            AND selection.offering.studyPlan.trainingPath.active = true
            AND selection.offering.period.academicYear.id = course.academicYear.id
            AND selection.offering.studyPlan.id = course.studyPlanSpace.studyPlan.id
            AND (selection.academicLevel.id = course.studyPlanSpace.academicLevel.id
              OR selection.academicLevel IS NULL AND course.studyPlanSpace.academicLevel IS NULL))
        AND (:search IS NULL OR UNACCENT_LOWER(course.studyPlanSpace.academicSpace.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(course.studyPlanSpace.studyPlan.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(COALESCE(instrument.name, '')) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      ORDER BY level.displayOrder NULLS LAST, course.studyPlanSpace.academicSpace.name, course.id
      """)
  Page<Course> findOfferedForPeriod(
      UUID institutionId,
      UUID trainingPathId,
      UUID periodId,
      @Nullable String search,
      Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "studyPlanSpace",
        "studyPlanSpace.studyPlan",
        "studyPlanSpace.studyPlan.trainingPath",
        "studyPlanSpace.academicSpace",
        "studyPlanSpace.academicLevel",
        "academicYear",
        "instrument"
      })
  @Query(
      """
      SELECT course FROM Course course
      LEFT JOIN course.instrument instrument
      LEFT JOIN course.studyPlanSpace.academicLevel level
      WHERE course.institution.id = :institutionId
        AND course.studyPlanSpace.studyPlan.trainingPath.id = :trainingPathId
        AND course.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.ACTIVE
        AND course.deletedAt IS NULL AND EXISTS (SELECT selection.id FROM EnrollmentPeriodOfferingLevel selection
          WHERE (:periodId IS NULL OR selection.offering.period.id = :periodId)
            AND selection.offering.period.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus.OPEN
            AND selection.offering.period.deletedAt IS NULL
            AND selection.offering.period.startDate <= :now AND selection.offering.period.endDate >= :now
            AND selection.offering.period.scopeConfigured = true
            AND selection.offering.studyPlan.deletedAt IS NULL
            AND selection.offering.studyPlan.trainingPath.deletedAt IS NULL
            AND selection.offering.studyPlan.trainingPath.active = true
            AND selection.offering.period.academicYear.id = course.academicYear.id
            AND selection.offering.studyPlan.id = course.studyPlanSpace.studyPlan.id
            AND (selection.academicLevel.id = course.studyPlanSpace.academicLevel.id
              OR selection.academicLevel IS NULL AND course.studyPlanSpace.academicLevel IS NULL))
        AND (:studyPlanSpaceId IS NULL OR course.studyPlanSpace.id = :studyPlanSpaceId)
        AND (:academicYear IS NULL OR course.academicYear.year = :academicYear)
        AND NOT EXISTS (SELECT requested.id FROM EnrollmentApplicationCourse requested
          WHERE requested.institution.id = :institutionId
            AND requested.course.id = course.id
            AND requested.enrollmentApplication.id <> :applicationId
            AND requested.enrollmentApplication.applicantPerson.id = :personId
            AND requested.enrollmentApplication.deletedAt IS NULL
            AND requested.enrollmentApplication.status NOT IN (
              ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.CANCELLED,
              ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus.REJECTED)
            AND requested.status IN (
              ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus.PENDING,
              ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus.WAITLISTED))
        AND NOT EXISTS (SELECT enrollment.id FROM CourseEnrollment enrollment
          WHERE enrollment.institution.id = :institutionId
            AND enrollment.course.id = course.id
            AND enrollment.student.person.id = :personId
            AND enrollment.status = ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus.ENROLLED)
        AND (:search IS NULL OR UNACCENT_LOWER(course.studyPlanSpace.academicSpace.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(course.studyPlanSpace.studyPlan.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(COALESCE(instrument.name, '')) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      ORDER BY level.displayOrder NULLS LAST, course.studyPlanSpace.academicSpace.name, course.id
      """)
  Page<Course> findOpenForEnrollment(
      UUID institutionId,
      UUID trainingPathId,
      UUID applicationId,
      UUID personId,
      @Nullable UUID periodId,
      java.time.Instant now,
      @Nullable UUID studyPlanSpaceId,
      @Nullable Integer academicYear,
      @Nullable String search,
      Pageable pageable);
}
