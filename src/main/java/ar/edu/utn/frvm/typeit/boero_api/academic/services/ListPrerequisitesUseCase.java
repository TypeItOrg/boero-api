package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.PrerequisiteRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.PrerequisiteResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPrerequisitesUseCase {
  private final PrerequisiteRepository prerequisiteRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;

  @Transactional(readOnly = true)
  public List<PrerequisiteResponse> execute(final UUID institutionId, final UUID targetId) {
    studyPlanSpaceRepository
        .findByIdAndInstitution_Id(targetId, institutionId)
        .orElseThrow(StudyPlanSpaceNotFoundException::new);
    return prerequisiteRepository.findByTargetStudyPlanSpace_Id(targetId).stream()
        .map(PrerequisiteResponse::from)
        .toList();
  }
}
