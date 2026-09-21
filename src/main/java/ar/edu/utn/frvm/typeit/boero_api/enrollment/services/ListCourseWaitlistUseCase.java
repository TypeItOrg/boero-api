package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.ScopedResource;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AcademicAccessGuard;
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
  private final AcademicAccessGuard accessGuard;

  private final EnrollmentApplicationCourseRepository applicationCourseRepository;
  private final CourseRepository courses;
  private final CourseCapacityService capacityService;

  @Transactional(readOnly = true)
  public List<CourseWaitlistEntryResponse> execute(final UUID institutionId, final UUID courseId) {
    accessGuard.require(
        PermissionCode.COURSE_WAITLIST_READ, institutionId, ScopedResource.COURSE, courseId);

    final var course =
        courses
            .findByIdAndInstitution_Id(courseId, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    final boolean hasCapacity = capacityService.hasCapacity(course);
    return applicationCourseRepository
        .findWaitlistedByCourseIdAndInstitutionIdOrderByWaitlistNumber(courseId, institutionId)
        .stream()
        .map(selection -> CourseWaitlistEntryResponse.from(selection, hasCapacity))
        .toList();
  }
}
