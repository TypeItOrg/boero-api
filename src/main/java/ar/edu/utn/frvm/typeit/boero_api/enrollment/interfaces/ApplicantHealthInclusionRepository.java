package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantHealthInclusion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantHealthInclusionRepository
    extends JpaRepository<ApplicantHealthInclusion, UUID> {

  Optional<ApplicantHealthInclusion> findByEnrollmentApplicationIdAndDeletedAtIsNull(
      UUID enrollmentApplicationId);
}
