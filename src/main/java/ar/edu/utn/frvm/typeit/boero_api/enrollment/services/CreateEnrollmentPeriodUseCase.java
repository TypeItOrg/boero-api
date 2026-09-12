package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentPeriodDatesException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CreateEnrollmentPeriodRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentPeriodResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateEnrollmentPeriodUseCase {

  private final EnrollmentPeriodRepository periodRepository;
  private final InstitutionRepository institutionRepository;
  private final AcademicYearRepository academicYearRepository;

  @Transactional
  public EnrollmentPeriodResponse execute(
      final UUID institutionId, final CreateEnrollmentPeriodRequest request) {
    if (request.startDate().isAfter(request.endDate())) {
      throw new InvalidEnrollmentPeriodDatesException();
    }

    final var institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);

    final var academicYear =
        academicYearRepository
            .findById(request.academicYearId())
            .filter(ay -> ay.getInstitution().getId().equals(institutionId))
            .filter(ay -> ay.getDeletedAt() == null)
            .orElseThrow(AcademicYearNotFoundException::new);

    final var period =
        EnrollmentPeriod.builder()
            .institution(institution)
            .academicYear(academicYear)
            .name(request.name().trim())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .status(EnrollmentPeriodStatus.PLANNED)
            .build();

    final var saved = periodRepository.save(period);
    return EnrollmentPeriodResponse.from(saved);
  }
}
