package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassDay;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceFormat;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassDayRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassScheduleRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseWaitlistSequence;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.WaitlistReason;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentScheduleRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseWaitlistSequenceRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationCourseRepository;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentApplicationCourseApprovalService {

  private final EnrollmentApplicationCourseRepository applicationCourseRepository;
  private final CourseClassRepository courseClassRepository;
  private final CourseClassDayRepository courseClassDayRepository;
  private final CourseClassScheduleRepository courseClassScheduleRepository;
  private final CourseEnrollmentScheduleRepository enrollmentScheduleRepository;
  private final CourseWaitlistSequenceRepository waitlistSequenceRepository;
  private final Clock clock;

  @Transactional
  public void process(final EnrollmentApplication application) {
    final List<EnrollmentApplicationCourse> selections =
        applicationCourseRepository.findByApplicationIdAndStatuses(
            application.getId(), List.of(EnrollmentApplicationCourseStatus.PENDING));
    selections.stream()
        .sorted(
            Comparator.comparing(
                    EnrollmentApplicationCourse::getRequestedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EnrollmentApplicationCourse::getId))
        .forEach(this::evaluate);
  }

  @Transactional
  public void reevaluateApprovedPendingForCourse(final UUID institutionId, final UUID courseId) {
    final var selections =
        applicationCourseRepository.findApprovedPendingByCourse(
            institutionId, courseId, EnrollmentApplicationCourseStatus.PENDING);
    if (selections.isEmpty()) {
      return;
    }

    final boolean hasCapacity = hasCapacity(selections.getFirst());
    selections.forEach(selection -> evaluate(selection, hasCapacity));
  }

  public void markSubmitted(final EnrollmentApplication application) {
    if (application.getCourseSelections() == null) {
      return;
    }
    for (final EnrollmentApplicationCourse selection : application.getCourseSelections()) {
      selection.markRequested(clock.instant(), hasCapacity(selection));
    }
  }

  private void evaluate(final EnrollmentApplicationCourse selection) {
    evaluate(selection, null);
  }

  private void evaluate(
      final EnrollmentApplicationCourse selection, final Boolean capacityOverride) {
    final Course course = selection.getCourse();
    if (course.isClosed()) {
      selection.reject(
          "COURSE_FINISHED", EnrollmentMessages.COURSE_NOT_ACTIVE, clock.instant(), null);
      return;
    }
    if (!course.isActive()) {
      return;
    }

    final boolean hasCapacity =
        capacityOverride == null ? hasCapacity(selection) : capacityOverride;
    if (selection.getSubmittedWithCapacity() == null) {
      selection.markRequested(clock.instant(), hasCapacity);
    }
    if (hasCapacity) {
      return;
    }

    final var sequence =
        waitlistSequenceRepository
            .findByCourseId(course.getId())
            .orElseGet(
                () ->
                    waitlistSequenceRepository.save(
                        CourseWaitlistSequence.create(course, selection.getInstitution())));
    final int waitlistNumber = sequence.nextNumber();
    selection.waitlist(
        waitlistNumber, WaitlistReason.NO_CAPACITY_AT_PARENT_APPROVAL, clock.instant());
  }

  private boolean hasCapacity(final EnrollmentApplicationCourse selection) {
    final Course course = selection.getCourse();
    final var classes = courseClassRepository.findByCourse_IdOrderByIdAsc(course.getId());
    final var classIds = classes.stream().map(value -> value.getId()).toList();
    final var days = courseClassDayRepository.findByCourseClass_IdIn(classIds);
    final var dayIds = days.stream().map(CourseClassDay::getId).toList();
    if (dayIds.isEmpty()) {
      return false;
    }
    final var schedules = courseClassScheduleRepository.findByDay_IdIn(dayIds);
    final var activeAssignments =
        enrollmentScheduleRepository.findActiveByDays(selection.getInstitution().getId(), dayIds);
    final Map<UUID, Long> occupiedByDay =
        activeAssignments.stream()
            .collect(
                Collectors.groupingBy(
                    value -> value.getSchedule().getDay().getId(), Collectors.counting()));
    final Map<UUID, Long> occupiedBySchedule =
        activeAssignments.stream()
            .collect(
                Collectors.groupingBy(value -> value.getSchedule().getId(), Collectors.counting()));
    final var schedulesByDay =
        schedules.stream().collect(Collectors.groupingBy(value -> value.getDay().getId()));

    for (final var day : days) {
      if (course.getAcademicSpace().getFormat() == AcademicSpaceFormat.GRUPAL) {
        if (day.getCapacity() == null
            || occupiedByDay.getOrDefault(day.getId(), 0L) < day.getCapacity()) {
          return true;
        }
        continue;
      }

      final int duration =
          day.getPeriodDurationMinutes() == null ? 0 : day.getPeriodDurationMinutes();
      if (duration <= 0) {
        continue;
      }
      final long configuredSlots =
          schedulesByDay.getOrDefault(day.getId(), List.of()).stream()
              .mapToLong(schedule -> schedule.durationMinutes() / duration)
              .sum();
      final long occupiedSlots =
          schedulesByDay.getOrDefault(day.getId(), List.of()).stream()
              .mapToLong(schedule -> occupiedBySchedule.getOrDefault(schedule.getId(), 0L))
              .sum();
      if (configuredSlots > occupiedSlots) {
        return true;
      }
    }

    return false;
  }
}
