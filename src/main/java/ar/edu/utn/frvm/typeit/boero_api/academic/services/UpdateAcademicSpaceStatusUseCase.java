package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.ActiveStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAcademicSpaceStatusUseCase {
  private final AcademicSpaceRepository academicSpaceRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID id, final ActiveStatusRequest request) {
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    if (!request.active() && academicSpaceRepository.existsInEditableOrActivePlan(id)) {
      throw new AcademicConflictException(AcademicMessages.MODIFICATION_NOT_ALLOWED);
    }
    space.updateStatus(request.active());
    academicSpaceRepository.flush();
  }
}
