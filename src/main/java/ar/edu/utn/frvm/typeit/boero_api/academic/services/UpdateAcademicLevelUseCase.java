package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicLevelNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicLevelResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateAcademicLevelRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAcademicLevelUseCase {
  private final AcademicLevelRepository academicLevelRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;

  @Transactional
  public AcademicLevelResponse execute(
      final UUID institutionId, final UUID id, final UpdateAcademicLevelRequest request) {
    final var level =
        academicLevelRepository
            .findByIdAndStudyPlan_Institution_Id(id, institutionId)
            .orElseThrow(AcademicLevelNotFoundException::new);
    final var plan = studyPlanDraftGuard.lock(institutionId, level.getStudyPlan().getId());
    final var name = AcademicNameNormalizer.display(request.name());
    if (academicLevelRepository.existsByNormalizedNameAndIdNot(plan.getId(), name, id)) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    if (academicLevelRepository.existsByStudyPlan_IdAndDisplayOrderAndIdNot(
        plan.getId(), request.displayOrder(), id)) {
      throw AcademicConflictException.forField("displayOrder", AcademicMessages.DUPLICATE_ORDER);
    }
    level.update(name, request.displayOrder(), request.description());
    try {
      academicLevelRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return AcademicLevelResponse.from(level);
  }
}
