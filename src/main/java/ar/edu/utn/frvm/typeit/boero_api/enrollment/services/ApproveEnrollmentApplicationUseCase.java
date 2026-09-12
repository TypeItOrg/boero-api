package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Student;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.StudentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveEnrollmentApplicationUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;
  private final StudentRepository studentRepository;

  @Transactional
  public EnrollmentApplicationResponse execute(
      final UUID institutionId, final UUID applicationId, final UUID resolvedByPersonId) {
    final var application =
        enrollmentApplicationRepository
            .findByIdAndInstitutionIdForUpdate(institutionId, applicationId)
            .orElseThrow(EnrollmentApplicationNotFoundException::new);

    application.approve(Instant.now(), resolvedByPersonId);

    if (!studentRepository.existsByInstitution_IdAndPerson_Id(
        institutionId, application.getApplicantPerson().getId())) {
      studentRepository.save(
          Student.builder()
              .institution(application.getInstitution())
              .person(application.getApplicantPerson())
              .fileNumber(generateFileNumber())
              .enrollmentDate(LocalDate.now())
              .build());
    }
    return EnrollmentApplicationResponse.from(application);
  }

  private String generateFileNumber() {
    final int year = Year.now().getValue();
    final long sequence = studentRepository.nextFileNumberSequenceValue();
    return String.format("%d-%05d", year, sequence);
  }
}
