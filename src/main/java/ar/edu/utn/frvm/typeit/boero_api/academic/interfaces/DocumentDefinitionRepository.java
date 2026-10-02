package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.DocumentDefinition;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentDefinitionRepository
    extends JpaRepository<DocumentDefinition, UUID>, JpaSpecificationExecutor<DocumentDefinition> {
  Optional<DocumentDefinition> findByIdAndInstitutionId(UUID id, UUID institutionId);
}
