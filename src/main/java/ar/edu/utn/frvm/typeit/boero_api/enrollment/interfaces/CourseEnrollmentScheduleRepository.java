package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollmentSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEnrollmentScheduleRepository
    extends JpaRepository<CourseEnrollmentSchedule, UUID> {

  List<CourseEnrollmentSchedule> findByCourseEnrollment_Id(UUID courseEnrollmentId);

  List<CourseEnrollmentSchedule> findByCourseEnrollment_IdIn(List<UUID> courseEnrollmentIds);

  @Query(
      "SELECT assignment FROM CourseEnrollmentSchedule assignment WHERE assignment.institution.id = :institutionId AND assignment.releasedAt IS NULL AND assignment.courseEnrollment.student.id = :studentId")
  @EntityGraph(attributePaths = {"courseEnrollment", "courseEnrollment.course.academicYear"})
  List<CourseEnrollmentSchedule> findActiveByStudent(
      @Param("institutionId") UUID institutionId, @Param("studentId") UUID studentId);

  @Query(
      "SELECT assignment FROM CourseEnrollmentSchedule assignment WHERE assignment.institution.id = :institutionId AND assignment.releasedAt IS NULL AND assignment.schedule.id = :scheduleId")
  List<CourseEnrollmentSchedule> findActiveBySchedule(
      @Param("institutionId") UUID institutionId, @Param("scheduleId") UUID scheduleId);

  @Query(
      "SELECT assignment FROM CourseEnrollmentSchedule assignment WHERE assignment.institution.id = :institutionId AND assignment.releasedAt IS NULL AND assignment.schedule.day.id = :dayId")
  List<CourseEnrollmentSchedule> findActiveByDay(
      @Param("institutionId") UUID institutionId, @Param("dayId") UUID dayId);

  @Query(
      "SELECT assignment FROM CourseEnrollmentSchedule assignment WHERE assignment.institution.id = :institutionId AND assignment.releasedAt IS NULL AND assignment.schedule.day.id IN :dayIds")
  List<CourseEnrollmentSchedule> findActiveByDays(
      @Param("institutionId") UUID institutionId, @Param("dayIds") List<UUID> dayIds);

  @Query(
      "SELECT assignment FROM CourseEnrollmentSchedule assignment WHERE assignment.institution.id = :institutionId AND assignment.releasedAt IS NULL AND assignment.individualSlot.id = :slotId")
  List<CourseEnrollmentSchedule> findActiveByIndividualSlot(
      @Param("institutionId") UUID institutionId, @Param("slotId") UUID slotId);
}
