package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentPeriodDatesException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentPeriodResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentPeriodRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateEnrollmentPeriodUseCase {

  private final EnrollmentPeriodRepository periodRepository;

  @Transactional
  public EnrollmentPeriodResponse execute(
      final UUID institutionId, final UUID periodId, final UpdateEnrollmentPeriodRequest request) {
    if (request.startDate().isAfter(request.endDate())) {
      throw new InvalidEnrollmentPeriodDatesException();
    }

    final var period =
        periodRepository
            .findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId)
            .orElseThrow(EnrollmentPeriodNotFoundException::new);

    period.setName(request.name().trim());
    period.setStartDate(request.startDate());
    period.setEndDate(request.endDate());

    final var saved = periodRepository.save(period);
    return EnrollmentPeriodResponse.from(saved);
  }
}
