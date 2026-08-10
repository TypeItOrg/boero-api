package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AcademicYearRepository
    extends JpaRepository<AcademicYear, UUID>, JpaSpecificationExecutor<AcademicYear> {
  Optional<AcademicYear> findByIdAndInstitution_Id(UUID id, UUID institutionId);

  boolean existsByInstitution_IdAndYear(UUID institutionId, int year);

  boolean existsByInstitution_IdAndYearAndIdNot(UUID institutionId, int year, UUID id);

  boolean existsByInstitution_IdAndStatus(UUID institutionId, AcademicYearStatus status);
}
