package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CreateTrainingPathRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TrainingPathResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateTrainingPathUseCase {
  private final TrainingPathRepository trainingPathRepository;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public TrainingPathResponse execute(
      final UUID institutionId, final CreateTrainingPathRequest request) {
    final var institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);
    final var name = AcademicNameNormalizer.display(request.name());
    if (trainingPathRepository.existsByNormalizedName(institutionId, name)) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    try {
      final var saved =
          trainingPathRepository.save(
              TrainingPath.create(institution, name, request.description()));
      trainingPathRepository.flush();
      return TrainingPathResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }
}
