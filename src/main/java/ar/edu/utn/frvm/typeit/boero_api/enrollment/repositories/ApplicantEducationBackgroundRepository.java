package ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantEducationBackground;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantEducationBackgroundRepository
    extends JpaRepository<ApplicantEducationBackground, UUID> {

  Optional<ApplicantEducationBackground> findByEnrollmentApplicationIdAndDeletedAtIsNull(
      UUID enrollmentApplicationId);
}
