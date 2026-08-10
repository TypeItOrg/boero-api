package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Prerequisite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrerequisiteRepository extends JpaRepository<Prerequisite, UUID> {
  List<Prerequisite> findByStudyPlan_Id(UUID studyPlanId);

  List<Prerequisite> findByTargetStudyPlanSpace_Id(UUID targetId);

  Optional<Prerequisite> findByIdAndStudyPlan_Institution_Id(UUID id, UUID institutionId);

  boolean existsByTargetStudyPlanSpace_IdOrRequiredStudyPlanSpace_Id(UUID id, UUID requiredId);
}
