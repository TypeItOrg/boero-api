package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseClassRepository extends JpaRepository<CourseClass, UUID> {
  List<CourseClass> findByCourse_IdOrderByIdAsc(UUID courseId);

  @Modifying
  @Query(
      "DELETE FROM CourseClassSchedule schedule WHERE schedule.day.courseClass.course.id = :courseId")
  void deleteSchedulesByCourseId(@Param("courseId") UUID courseId);

  @Modifying
  @Query("DELETE FROM CourseClassTeacher teacher WHERE teacher.courseClass.course.id = :courseId")
  void deleteTeachersByCourseId(@Param("courseId") UUID courseId);

  @Modifying
  @Query("DELETE FROM CourseClassDay day WHERE day.courseClass.course.id = :courseId")
  void deleteDaysByCourseId(@Param("courseId") UUID courseId);

  @Modifying
  @Query("DELETE FROM CourseClass courseClass WHERE courseClass.course.id = :courseId")
  void deleteByCourseId(@Param("courseId") UUID courseId);
}
