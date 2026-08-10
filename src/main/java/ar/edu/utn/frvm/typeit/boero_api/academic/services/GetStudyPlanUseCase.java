package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetStudyPlanUseCase {
  private final StudyPlanRepository studyPlanRepository;

  @Transactional(readOnly = true)
  public StudyPlanResponse execute(final UUID institutionId, final UUID id) {
    return studyPlanRepository
        .findByIdAndInstitution_Id(id, institutionId)
        .map(StudyPlanResponse::from)
        .orElseThrow(StudyPlanNotFoundException::new);
  }
}
