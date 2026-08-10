package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.PrerequisiteNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.PrerequisiteRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.PrerequisiteResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPrerequisiteUseCase {
  private final PrerequisiteRepository prerequisiteRepository;

  @Transactional(readOnly = true)
  public PrerequisiteResponse execute(final UUID institutionId, final UUID id) {
    return prerequisiteRepository
        .findByIdAndStudyPlan_Institution_Id(id, institutionId)
        .map(PrerequisiteResponse::from)
        .orElseThrow(PrerequisiteNotFoundException::new);
  }
}
