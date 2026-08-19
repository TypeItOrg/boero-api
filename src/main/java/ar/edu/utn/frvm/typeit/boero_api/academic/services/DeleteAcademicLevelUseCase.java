package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicLevelNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteAcademicLevelUseCase {
  private final AcademicLevelRepository academicLevelRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;

  @Transactional
  public void execute(final UUID institutionId, final UUID id) {
    final var level =
        academicLevelRepository
            .findByIdAndStudyPlan_Institution_Id(id, institutionId)
            .orElseThrow(AcademicLevelNotFoundException::new);
    studyPlanDraftGuard.lock(institutionId, level.getStudyPlan().getId());
    if (studyPlanSpaceRepository.existsByAcademicLevel_Id(id)) {
      throw new AcademicConflictException(AcademicMessages.ACADEMIC_LEVEL_HAS_SPACES);
    }
    academicLevelRepository.delete(level);
  }
}
