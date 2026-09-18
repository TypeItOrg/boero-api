package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationCourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseWaitlistEntryResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCourseWaitlistUseCase {

  private final EnrollmentApplicationCourseRepository applicationCourseRepository;

  @Transactional(readOnly = true)
  public List<CourseWaitlistEntryResponse> execute(final UUID institutionId, final UUID courseId) {
    return applicationCourseRepository
        .findWaitlistedByCourseIdAndInstitutionIdOrderByWaitlistNumber(courseId, institutionId)
        .stream()
        .map(CourseWaitlistEntryResponse::from)
        .toList();
  }
}
