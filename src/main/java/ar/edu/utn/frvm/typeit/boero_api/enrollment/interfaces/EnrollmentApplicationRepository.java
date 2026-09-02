package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentApplicationRepository
    extends JpaRepository<EnrollmentApplication, UUID> {

  @EntityGraph(attributePaths = {"person", "institution", "studyPlan", "academicYear"})
  Optional<EnrollmentApplication> findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
      UUID id, UUID personId, UUID institutionId);

  @EntityGraph(attributePaths = {"person", "institution", "studyPlan", "academicYear"})
  List<EnrollmentApplication> findByPerson_IdAndInstitution_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID personId, UUID institutionId);
}
