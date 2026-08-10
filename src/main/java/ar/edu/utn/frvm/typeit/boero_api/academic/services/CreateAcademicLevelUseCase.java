package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLevel;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicLevelResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CreateAcademicLevelRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAcademicLevelUseCase {
  private final AcademicLevelRepository academicLevelRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;

  @Transactional
  public AcademicLevelResponse execute(
      final UUID institutionId, final UUID studyPlanId, final CreateAcademicLevelRequest request) {
    final var plan = studyPlanDraftGuard.lock(institutionId, studyPlanId);
    final var name = AcademicNameNormalizer.display(request.name());
    if (academicLevelRepository.existsByNormalizedName(studyPlanId, name)) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    if (academicLevelRepository.existsByStudyPlan_IdAndDisplayOrder(
        studyPlanId, request.displayOrder())) {
      throw AcademicConflictException.forField("displayOrder", AcademicMessages.DUPLICATE_ORDER);
    }
    try {
      final var saved =
          academicLevelRepository.save(
              AcademicLevel.create(plan, name, request.displayOrder(), request.description()));
      academicLevelRepository.flush();
      return AcademicLevelResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }
}
