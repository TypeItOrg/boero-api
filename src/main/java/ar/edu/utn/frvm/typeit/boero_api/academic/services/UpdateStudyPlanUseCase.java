package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateStudyPlanRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.ScopedResource;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AcademicAccessGuard;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateStudyPlanUseCase {
  private final AcademicAccessGuard accessGuard;
  private final StudyPlanRepository studyPlanRepository;

  @Transactional
  public StudyPlanResponse execute(
      final UUID institutionId, final UUID id, final UpdateStudyPlanRequest request) {
    accessGuard.require(
        PermissionCode.STUDY_PLAN_UPDATE, institutionId, ScopedResource.STUDY_PLAN, id);

    final var plan =
        studyPlanRepository
            .findByIdAndInstitution_IdForUpdate(id, institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    final var name = AcademicNameNormalizer.display(request.name());
    if (studyPlanRepository.existsByNormalizedNameAndVersionAndIdNot(
        plan.getTrainingPath().getId(), name, plan.getVersionNumber(), id)) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    plan.updateDraft(name, request.effectiveFrom(), request.effectiveTo());
    try {
      studyPlanRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return StudyPlanResponse.from(plan);
  }
}
