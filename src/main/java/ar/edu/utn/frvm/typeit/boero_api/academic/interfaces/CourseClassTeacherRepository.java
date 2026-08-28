package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassTeacher;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseClassTeacherRepository extends JpaRepository<CourseClassTeacher, UUID> {
  @EntityGraph(attributePaths = "person")
  List<CourseClassTeacher> findByCourseClass_IdIn(List<UUID> courseClassIds);
}
