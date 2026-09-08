package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteEnrollmentPeriodUseCase {

  private final EnrollmentPeriodRepository periodRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID periodId) {
    final var period =
        periodRepository
            .findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId)
            .orElseThrow(EnrollmentPeriodNotFoundException::new);

    period.markDeleted();
    periodRepository.save(period);
  }
}
