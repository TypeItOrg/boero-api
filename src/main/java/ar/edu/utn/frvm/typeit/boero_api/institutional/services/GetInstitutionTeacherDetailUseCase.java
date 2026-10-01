package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassTeacherRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.services.CourseTreeReader;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.TeacherDetailResponse;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetInstitutionTeacherDetailUseCase {
  private final PersonRepository personRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final UserRepository userRepository;
  private final CourseClassTeacherRepository courseClassTeacherRepository;
  private final CourseTreeReader courseTreeReader;

  @Transactional(readOnly = true)
  public TeacherDetailResponse execute(final UUID institutionId, final UUID teacherId) {
    final var person =
        personRepository
            .findWithDetailsByIdAndInstitution_Id(teacherId, institutionId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(PersonNotFoundException::new);
    if (!personRoleAssignmentRepository.existsByPerson_IdAndInstitution_IdAndRole_Code(
        teacherId, institutionId, SystemRoleCode.TEACHER.name())) {
      throw new PersonNotFoundException();
    }
    final boolean enabled =
        userRepository
            .findByPerson_IdAndInstitution_Id(teacherId, institutionId)
            .map(user -> user.isAccessEnabled())
            .orElse(false);
    final var classes =
        courseClassTeacherRepository.findAssignedClassesForDetail(institutionId, teacherId);
    final var details =
        courseTreeReader.readClasses(classes).stream()
            .collect(Collectors.toMap(courseClass -> courseClass.id(), Function.identity()));
    final var courses =
        classes.stream()
            .collect(Collectors.groupingBy(courseClass -> courseClass.getCourse().getId()))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  final var course = entry.getValue().getFirst().getCourse();
                  return new TeacherDetailResponse.CourseAssignmentResponse(
                      course.getId(),
                      course.getAcademicSpace().getName(),
                      course.getInstrument() == null ? null : course.getInstrument().getName(),
                      course.getAcademicYear().getYear(),
                      entry.getValue().stream()
                          .map(courseClass -> details.get(courseClass.getId()))
                          .toList());
                })
            .toList();
    return new TeacherDetailResponse(
        PersonResponse.from(person), enabled, SystemRoleCode.TEACHER.name(), courses);
  }
}
