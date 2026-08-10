package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLevel;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CreateStudyPlanSpaceRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateStudyPlanSpaceUseCase {
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final AcademicSpaceRepository academicSpaceRepository;
  private final AcademicLevelRepository academicLevelRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;

  @Transactional
  public StudyPlanSpaceResponse execute(
      final UUID institutionId, final UUID studyPlanId, final CreateStudyPlanSpaceRequest request) {
    final var plan = studyPlanDraftGuard.lock(institutionId, studyPlanId);
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_IdAndActiveTrue(request.academicSpaceId(), institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    final var level = resolveLevel(studyPlanId, request.academicLevelId());
    try {
      final var saved =
          studyPlanSpaceRepository.save(
              StudyPlanSpace.create(
                  plan.getInstitution(),
                  plan,
                  space,
                  level,
                  request.requirementType(),
                  request.displayOrder(),
                  request.approvalMode()));
      studyPlanSpaceRepository.flush();
      return StudyPlanSpaceResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
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
