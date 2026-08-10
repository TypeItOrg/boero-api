package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStudyPlanSpacesUseCase {
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final StudyPlanRepository studyPlanRepository;

  @Transactional(readOnly = true)
  public List<StudyPlanSpaceResponse> execute(final UUID institutionId, final UUID studyPlanId) {
    studyPlanRepository
        .findByIdAndInstitution_Id(studyPlanId, institutionId)
        .orElseThrow(StudyPlanNotFoundException::new);
    return studyPlanSpaceRepository.findByStudyPlanIdWithDetails(studyPlanId).stream()
        .map(StudyPlanSpaceResponse::from)
        .toList();
  }
}
