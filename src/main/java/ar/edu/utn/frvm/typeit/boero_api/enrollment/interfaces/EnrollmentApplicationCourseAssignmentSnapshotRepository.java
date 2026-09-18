package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourseAssignmentSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentApplicationCourseAssignmentSnapshotRepository
    extends JpaRepository<EnrollmentApplicationCourseAssignmentSnapshot, UUID> {

  List<EnrollmentApplicationCourseAssignmentSnapshot> findByApplicationCourse_Id(
      UUID applicationCourseId);
}
