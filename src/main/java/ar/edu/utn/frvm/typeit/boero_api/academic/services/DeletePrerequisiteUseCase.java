package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.PrerequisiteNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.PrerequisiteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePrerequisiteUseCase {
  private final PrerequisiteRepository prerequisiteRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;

  @Transactional
  public void execute(final UUID institutionId, final UUID id) {
    final var existing =
        prerequisiteRepository
            .findByIdAndStudyPlan_Institution_Id(id, institutionId)
            .orElseThrow(PrerequisiteNotFoundException::new);
    studyPlanDraftGuard.lock(institutionId, existing.getStudyPlan().getId());
    prerequisiteRepository.delete(existing);
  }
}
