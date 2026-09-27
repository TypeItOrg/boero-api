package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPathDocumentRequirement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingPathDocumentRequirementRepository
    extends JpaRepository<TrainingPathDocumentRequirement, UUID> {
  List<TrainingPathDocumentRequirement> findByTrainingPathIdOrderByDisplayOrderAscIdAsc(UUID id);

  Optional<TrainingPathDocumentRequirement> findByIdAndTrainingPathId(UUID id, UUID pathId);
}
