package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class EnrollmentDraftDataValidator {

  private final TrainingPathRepository trainingPathRepository;

  public void validate(final UUID institutionId, final JsonNode data) {
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
  }
}
