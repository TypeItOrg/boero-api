package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceFormat;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseDay;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassDayRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassScheduleRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassTeacherRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseClassDayRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseClassRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseClassScheduleRequest;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseClassAssemblerTest {

  @Mock private CourseClassRepository courseClassRepository;
  @Mock private CourseClassDayRepository courseClassDayRepository;
  @Mock private CourseClassScheduleRepository courseClassScheduleRepository;
  @Mock private CourseClassTeacherRepository courseClassTeacherRepository;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private PersonRepository personRepository;
  @Mock private Course course;
  @Mock private Person person;

  private CourseClassAssembler assembler;

  private final Institution institution = Institution.builder().id(UUID.randomUUID()).build();

  @BeforeEach
  void setUp() {
    assembler =
        new CourseClassAssembler(
            courseClassRepository,
            courseClassDayRepository,
            courseClassScheduleRepository,
            courseClassTeacherRepository,
            personRoleAssignmentRepository,
            personRepository);
    given(courseClassRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    given(courseClassDayRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    given(courseClassScheduleRepository.save(any()))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(personRepository.findByIdAndInstitution_Id(any(), any())).willReturn(Optional.of(person));
  }

  private void stubValidTeachers() {
    given(
            personRoleAssignmentRepository.countDistinctPersonsByInstitutionAndRoleCode(
                any(), anyList(), any()))
        .willReturn(1L);
  }

  private CourseClassRequest individualMondayClass() {
    return new CourseClassRequest(
        List.of(UUID.randomUUID()),
        List.of(
            new CourseClassDayRequest(
                CourseDay.MONDAY,
                null,
                60,
                List.of(
                    new CourseClassScheduleRequest(LocalTime.of(14, 0), LocalTime.of(18, 0)),
                    new CourseClassScheduleRequest(LocalTime.of(18, 30), LocalTime.of(19, 30))))));
  }

  @Test
  @DisplayName("Should assemble classes and compute capacity for individual spaces")
  void assemblesIndividualClassesWithComputedCapacity() {
    stubValidTeachers();

    final var classes =
        assembler.assemble(
            institution, course, AcademicSpaceFormat.INDIVIDUAL, List.of(individualMondayClass()));

    assertThat(classes).hasSize(1);
    verifySavedCapacity(5);
  }

  @Test
  @DisplayName("Should keep optional grupal capacity and clear the period duration")
  void keepsGrupalOptionalCapacity() {
    stubValidTeachers();
    final var request =
        new CourseClassRequest(
            List.of(UUID.randomUUID()),
            List.of(
                new CourseClassDayRequest(
                    CourseDay.TUESDAY,
                    null,
                    null,
                    List.of(
                        new CourseClassScheduleRequest(LocalTime.of(8, 0), LocalTime.of(10, 0))))));

    final var classes =
        assembler.assemble(institution, course, AcademicSpaceFormat.GRUPAL, List.of(request));

    assertThat(classes).hasSize(1);
    verifySavedCapacity(null);
  }

  @Test
  @DisplayName("Should reject schedules whose total duration is not divisible by the period")
  void rejectsIndivisibleSchedules() {
    stubValidTeachers();
    final var request =
        new CourseClassRequest(
            List.of(UUID.randomUUID()),
            List.of(
                new CourseClassDayRequest(
                    CourseDay.WEDNESDAY,
                    null,
                    60,
                    List.of(
                        new CourseClassScheduleRequest(
                            LocalTime.of(14, 0), LocalTime.of(16, 30))))));

    assertThatThrownBy(
            () ->
                assembler.assemble(
                    institution, course, AcademicSpaceFormat.INDIVIDUAL, List.of(request)))
        .isInstanceOf(AcademicValidationException.class);
  }

  @Test
  @DisplayName("Should reject overlapping schedules within the same day")
  void rejectsOverlappingSchedules() {
    stubValidTeachers();
    final var request =
        new CourseClassRequest(
            List.of(UUID.randomUUID()),
            List.of(
                new CourseClassDayRequest(
                    CourseDay.THURSDAY,
                    10,
                    null,
                    List.of(
                        new CourseClassScheduleRequest(LocalTime.of(14, 0), LocalTime.of(18, 0)),
                        new CourseClassScheduleRequest(
                            LocalTime.of(17, 0), LocalTime.of(19, 0))))));

    assertThatThrownBy(
            () ->
                assembler.assemble(
                    institution, course, AcademicSpaceFormat.GRUPAL, List.of(request)))
        .isInstanceOf(AcademicValidationException.class);
  }

  @Test
  @DisplayName("Should allow contiguous schedules within the same day")
  void allowsContiguousSchedules() {
    stubValidTeachers();
    final var request =
        new CourseClassRequest(
            List.of(UUID.randomUUID()),
            List.of(
                new CourseClassDayRequest(
                    CourseDay.THURSDAY,
                    10,
                    null,
                    List.of(
                        new CourseClassScheduleRequest(LocalTime.of(14, 0), LocalTime.of(16, 0)),
                        new CourseClassScheduleRequest(
                            LocalTime.of(16, 0), LocalTime.of(18, 0))))));

    final var classes =
        assembler.assemble(institution, course, AcademicSpaceFormat.GRUPAL, List.of(request));

    assertThat(classes).hasSize(1);
  }

  @Test
  @DisplayName("Should reject a day configured twice in the same class")
  void rejectsDuplicatedDays() {
    stubValidTeachers();
    final var day =
        new CourseClassDayRequest(
            CourseDay.FRIDAY,
            10,
            null,
            List.of(new CourseClassScheduleRequest(LocalTime.of(8, 0), LocalTime.of(10, 0))));

    assertThatThrownBy(
            () ->
                assembler.assemble(
                    institution,
                    course,
                    AcademicSpaceFormat.GRUPAL,
                    List.of(new CourseClassRequest(List.of(UUID.randomUUID()), List.of(day, day)))))
        .isInstanceOf(AcademicValidationException.class);
  }

  @Test
  @DisplayName("Should reject teachers without the teacher role")
  void rejectsInvalidTeachers() {
    given(
            personRoleAssignmentRepository.countDistinctPersonsByInstitutionAndRoleCode(
                any(), anyList(), any()))
        .willReturn(0L);

    assertThatThrownBy(
            () ->
                assembler.assemble(
                    institution,
                    course,
                    AcademicSpaceFormat.GRUPAL,
                    List.of(individualMondayClass())))
        .isInstanceOf(AcademicValidationException.class);
  }

  private void verifySavedCapacity(final Integer expected) {
    verify(courseClassDayRepository, atLeastOnce())
        .save(
            argThat(
                day ->
                    expected == null
                        ? day.getCapacity() == null
                        : expected.equals(day.getCapacity())));
  }
}
