package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLevel;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateStudyPlanSpaceRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateStudyPlanSpaceUseCase {
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final AcademicSpaceRepository academicSpaceRepository;
  private final AcademicLevelRepository academicLevelRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;

  @Transactional
  public StudyPlanSpaceResponse execute(
      final UUID institutionId, final UUID id, final UpdateStudyPlanSpaceRequest request) {
    final var existing =
        studyPlanSpaceRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(StudyPlanSpaceNotFoundException::new);
    final var plan = studyPlanDraftGuard.lock(institutionId, existing.getStudyPlan().getId());
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(
                request.academicSpaceId(), institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    final var level = resolveLevel(plan.getId(), request.academicLevelId());
    existing.update(
        space, level, request.requirementType(), request.displayOrder(), request.approvalMode());
    try {
      studyPlanSpaceRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return StudyPlanSpaceResponse.from(existing);
  }

  private AcademicLevel resolveLevel(final UUID studyPlanId, final UUID academicLevelId) {
    if (academicLevelId == null) {
      return null;
    }
    return academicLevelRepository
        .findByIdAndStudyPlan_Id(academicLevelId, studyPlanId)
        .orElseThrow(() -> new AcademicConflictException(AcademicMessages.INVALID_RELATIONSHIP));
  }
}
