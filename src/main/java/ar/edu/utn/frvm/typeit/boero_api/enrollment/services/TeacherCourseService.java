package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassTeacherRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseClassResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.services.CourseTreeReader;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.TeacherCourseClassResponse;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherCourseService {
  private final CourseClassTeacherRepository assignments;
  private final PersonRoleAssignmentRepository roles;
  private final CourseTreeReader treeReader;
  private final CourseEnrollmentService enrollments;

  public PaginatedResponse<TeacherCourseClassResponse> list(
      final UUID institutionId, final UUID personId, final Pageable pageable) {
    requireTeacher(institutionId, personId);
    final var page = assignments.findAssignedClasses(institutionId, personId, pageable);
    final var details =
        treeReader.readClasses(page.getContent()).stream()
            .collect(Collectors.toMap(CourseClassResponse::id, value -> value));
    return PaginatedResponse.from(
        page.map(
            courseClass -> {
              final var course = courseClass.getCourse();
              final var detail = details.get(courseClass.getId());
              return new TeacherCourseClassResponse(
                  course.getId(),
                  course.getAcademicSpace().getName(),
                  course.getInstrument() == null ? null : course.getInstrument().getName(),
                  courseClass.displayName(),
                  detail);
            }));
  }

  public PaginatedResponse<CourseEnrollmentResponse> listEnrollments(
      final UUID institutionId, final UUID personId, final UUID classId, final Pageable pageable) {
    requireTeacher(institutionId, personId);
    if (!assignments.existsByInstitution_IdAndPerson_IdAndCourseClass_Id(
        institutionId, personId, classId)) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_CLASS_INVALID);
    }
    return enrollments.listForTeacherClass(institutionId, personId, classId, pageable);
  }

  private void requireTeacher(final UUID institutionId, final UUID personId) {
    if (!roles.existsByPerson_IdAndInstitution_IdAndRole_Code(
        personId, institutionId, SystemRoleCode.TEACHER.name())) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
  }
}
