package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicLevelResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAcademicLevelsUseCase {
  private final AcademicLevelRepository academicLevelRepository;
  private final StudyPlanRepository studyPlanRepository;

  @Transactional(readOnly = true)
  public List<AcademicLevelResponse> execute(final UUID institutionId, final UUID studyPlanId) {
    studyPlanRepository
        .findByIdAndInstitution_Id(studyPlanId, institutionId)
        .orElseThrow(StudyPlanNotFoundException::new);
    return academicLevelRepository.findByStudyPlan_IdOrderByDisplayOrderAsc(studyPlanId).stream()
        .map(AcademicLevelResponse::from)
        .toList();
  }
}
