package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class EnrollmentDraftDataValidator {

  private final TrainingPathRepository trainingPathRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;

  public void validate(
      final UUID institutionId, final EnrollmentApplication application, final JsonNode data) {
    if (data == null) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_DATA_REQUIRED,
          Map.of("data", EnrollmentMessages.ENROLLMENT_APPLICATION_DATA_REQUIRED));
    }
    if (!data.isObject()) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_DATA_INVALID,
          Map.of("data", EnrollmentMessages.ENROLLMENT_APPLICATION_DATA_INVALID));
    }
    final JsonNode trainingPathNode = data.path("careerSelection").path("trainingPathId");
    if (trainingPathNode.isMissingNode() || trainingPathNode.isNull()) {
      validateStudyPlanSpaceSelection(institutionId, application, data);
      return;
    }
    final UUID trainingPathId;
    try {
      trainingPathId = UUID.fromString(trainingPathNode.asText());
    } catch (IllegalArgumentException exception) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID,
          Map.of(
              "data.careerSelection.trainingPathId",
              EnrollmentMessages.ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID));
    }
    trainingPathRepository
        .findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(trainingPathId, institutionId)
        .orElseThrow(TrainingPathNotFoundException::new);
    validateStudyPlanSpaceSelection(institutionId, application, data, trainingPathId);
    return;

    // unreachable, kept for clarity in the control flow above
  }

  private void validateStudyPlanSpaceSelection(
      final UUID institutionId, final EnrollmentApplication application, final JsonNode data) {
    validateStudyPlanSpaceSelection(institutionId, application, data, null);
  }

  private void validateStudyPlanSpaceSelection(
      final UUID institutionId,
      final EnrollmentApplication application,
      final JsonNode data,
      final UUID trainingPathId) {
    final UUID effectiveStudyPlanId = resolveEffectiveStudyPlanId(institutionId, application, trainingPathId);
    validateStudyPlanSpaceSelection(institutionId, effectiveStudyPlanId, data);
  }

  private void validateStudyPlanSpaceSelection(
      final UUID institutionId, final UUID studyPlanId, final JsonNode data) {
    final JsonNode studyPlanSpaceIdsNode =
        data.path("academicSpaceSelection").path("studyPlanSpaceIds");
    if (studyPlanSpaceIdsNode.isMissingNode() || studyPlanSpaceIdsNode.isNull()) {
      return;
    }
    if (!studyPlanSpaceIdsNode.isArray()) {
      throw invalidStudyPlanSpaces();
    }
    final List<UUID> studyPlanSpaceIds = new ArrayList<>();
    final Set<UUID> uniqueIds = new HashSet<>();
    for (final JsonNode studyPlanSpaceIdNode : studyPlanSpaceIdsNode) {
      if (!studyPlanSpaceIdNode.isTextual()) {
        throw invalidStudyPlanSpaces();
      }
      final UUID studyPlanSpaceId;
      try {
        studyPlanSpaceId = UUID.fromString(studyPlanSpaceIdNode.asText());
      } catch (IllegalArgumentException exception) {
        throw invalidStudyPlanSpaces();
      }
      if (!uniqueIds.add(studyPlanSpaceId)) {
        throw invalidStudyPlanSpaces();
      }
      studyPlanSpaceIds.add(studyPlanSpaceId);
    }
    if (studyPlanSpaceIds.isEmpty()) {
      return;
    }
    final int eligibleCount =
        studyPlanSpaceRepository
            .findEligibleByIdInAndStudyPlanId(institutionId, studyPlanId, studyPlanSpaceIds)
            .size();
    if (eligibleCount != studyPlanSpaceIds.size()) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACE_INVALID,
          Map.of(
              "data.academicSpaceSelection.studyPlanSpaceIds",
              EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACE_INVALID));
    }
  }

  private EnrollmentValidationException invalidStudyPlanSpaces() {
    return new EnrollmentValidationException(
        EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACES_INVALID,
        Map.of(
            "data.academicSpaceSelection.studyPlanSpaceIds",
            EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACES_INVALID));
  }

  private UUID resolveEffectiveStudyPlanId(
      final UUID institutionId,
      final EnrollmentApplication application,
      final UUID trainingPathId) {
    if (trainingPathId == null) {
      return application.getStudyPlan().getId();
    }

    final var validOn = application.getAcademicYear().getStartDate() != null
        ? application.getAcademicYear().getStartDate()
        : java.time.LocalDate.now();

    return studyPlanRepository
        .findActiveByTrainingPathIdAndInstitutionIdValidOn(
            trainingPathId,
            institutionId,
            ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus.ACTIVE,
            validOn)
        .stream()
        .findFirst()
        .orElseThrow(ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException::new)
        .getId();
  }
}
