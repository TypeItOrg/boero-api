package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentCourseOptionResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListEnrollmentApplicationCoursesUseCase {

  private final EnrollmentApplicationRepository applicationRepository;
  private final CourseRepository courseRepository;

  @Transactional(readOnly = true)
  public Page<EnrollmentCourseOptionResponse> execute(
      final UUID personId,
      final UUID applicationId,
      final @Nullable String search,
      final Pageable pageable) {
    final var application =
        applicationRepository
            .findById(applicationId)
            .filter(candidate -> candidate.getDeletedAt() == null)
            .filter(candidate -> candidate.getApplicantPerson().getId().equals(personId))
            .orElseThrow(EnrollmentApplicationNotFoundException::new);
    if (application.getTrainingPath() == null || application.getAcademicYear() == null) {
      return Page.empty(pageable);
    }

    return courseRepository
        .findActiveByTrainingPathAndAcademicYear(
            application.getInstitution().getId(),
            application.getTrainingPath().getId(),
            application.getAcademicYear().getId(),
            search == null || search.isBlank() ? null : search.trim(),
            pageable)
        .map(EnrollmentCourseOptionResponse::from);
  }
}
