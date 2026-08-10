package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.PrerequisiteNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.PrerequisiteRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.PrerequisiteResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdatePrerequisiteRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePrerequisiteUseCase {
  private final PrerequisiteRepository prerequisiteRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;
  private final PrerequisiteCycleValidator prerequisiteCycleValidator;

  @Transactional
  public PrerequisiteResponse execute(
      final UUID institutionId, final UUID id, final UpdatePrerequisiteRequest request) {
    final var existing =
        prerequisiteRepository
            .findByIdAndStudyPlan_Institution_Id(id, institutionId)
            .orElseThrow(PrerequisiteNotFoundException::new);
    final var target = existing.getTargetStudyPlanSpace();
    final var plan = studyPlanDraftGuard.lock(institutionId, existing.getStudyPlan().getId());
    final var required = requireSpace(institutionId, request.requiredStudyPlanSpaceId());
    validateSamePlan(target, required);
    prerequisiteCycleValidator.validate(plan.getId(), target.getId(), required.getId(), id);
    existing.update(required, request.requirementStage(), request.requiredCondition());
    try {
      prerequisiteRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return PrerequisiteResponse.from(existing);
  }

  private StudyPlanSpace requireSpace(final UUID institutionId, final UUID id) {
    return studyPlanSpaceRepository
        .findByIdAndInstitution_Id(id, institutionId)
        .orElseThrow(StudyPlanSpaceNotFoundException::new);
  }

  private static void validateSamePlan(final StudyPlanSpace target, final StudyPlanSpace required) {
    if (target.getId().equals(required.getId())
        || !target.getStudyPlan().getId().equals(required.getStudyPlan().getId())) {
      throw new AcademicConflictException(AcademicMessages.INVALID_RELATIONSHIP);
    }
  }
}
