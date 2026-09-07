package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.ShiftNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.ShiftRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.ActiveStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateShiftStatusUseCase {
  private final ShiftRepository shiftRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID id, final ActiveStatusRequest request) {
    final var shift =
        shiftRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(ShiftNotFoundException::new);
    shift.updateStatus(request.active());
    shiftRepository.flush();
  }
}
