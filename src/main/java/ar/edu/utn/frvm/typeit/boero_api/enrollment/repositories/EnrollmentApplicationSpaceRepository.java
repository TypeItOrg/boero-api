package ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationSpace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentApplicationSpaceRepository
    extends JpaRepository<EnrollmentApplicationSpace, UUID> {

  List<EnrollmentApplicationSpace>
      findByEnrollmentApplication_IdAndDeletedAtIsNull(UUID enrollmentApplicationId);

  void deleteByEnrollmentApplication_Id(UUID enrollmentApplicationId);
}
