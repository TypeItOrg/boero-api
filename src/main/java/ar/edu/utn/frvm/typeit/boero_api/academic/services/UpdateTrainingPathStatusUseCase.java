package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.ActiveStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateTrainingPathStatusUseCase {
  private final TrainingPathRepository trainingPathRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID id, final ActiveStatusRequest request) {
    final var path =
        trainingPathRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(TrainingPathNotFoundException::new);
    if (!request.active() && trainingPathRepository.existsActiveStudyPlan(id)) {
      throw new AcademicConflictException(AcademicMessages.TRAINING_PATH_HAS_ACTIVE_PLANS);
    }
    path.updateStatus(request.active());
    trainingPathRepository.flush();
  }
}
