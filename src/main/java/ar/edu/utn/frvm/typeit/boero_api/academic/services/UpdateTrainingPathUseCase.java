package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TrainingPathResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateTrainingPathRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateTrainingPathUseCase {
  private final TrainingPathRepository trainingPathRepository;

  @Transactional
  public TrainingPathResponse execute(
      final UUID institutionId, final UUID id, final UpdateTrainingPathRequest request) {
    final var path =
        trainingPathRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(TrainingPathNotFoundException::new);
    final var name = AcademicNameNormalizer.display(request.name());
    if (trainingPathRepository.existsByNormalizedNameAndIdNot(institutionId, name, id)) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    path.update(name, request.description());
    try {
      trainingPathRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return TrainingPathResponse.from(path);
  }
}
