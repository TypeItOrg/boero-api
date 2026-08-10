package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetStudyPlanSpaceUseCase {
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;

  @Transactional(readOnly = true)
  public StudyPlanSpaceResponse execute(final UUID institutionId, final UUID id) {
    return studyPlanSpaceRepository
        .findDetailsByIdAndInstitutionId(id, institutionId)
        .map(StudyPlanSpaceResponse::from)
        .orElseThrow(StudyPlanSpaceNotFoundException::new);
  }
}
