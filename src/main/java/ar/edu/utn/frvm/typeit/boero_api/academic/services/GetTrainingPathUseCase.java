package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TrainingPathResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTrainingPathUseCase {
  private final TrainingPathRepository trainingPathRepository;

  @Transactional(readOnly = true)
  public TrainingPathResponse execute(final UUID institutionId, final UUID id) {
    return trainingPathRepository
        .findByIdAndInstitution_Id(id, institutionId)
        .map(TrainingPathResponse::from)
        .orElseThrow(TrainingPathNotFoundException::new);
  }
}
