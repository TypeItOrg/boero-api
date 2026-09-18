package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassSchedule;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceFormat;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassDayRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassScheduleRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassTeacherRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseIndividualSlot;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseIndividualSlotRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentAssignmentOptionsResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentClassOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentDayOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentScheduleOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseIndividualSlotOptionResponse;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCourseEnrollmentOptionsUseCase {

  private final CourseRepository courseRepository;
  private final CourseClassRepository courseClassRepository;
  private final CourseClassDayRepository courseClassDayRepository;
  private final CourseClassScheduleRepository courseClassScheduleRepository;
  private final CourseClassTeacherRepository courseClassTeacherRepository;
  private final CourseIndividualSlotRepository courseIndividualSlotRepository;

  @Transactional
  public CourseEnrollmentAssignmentOptionsResponse execute(
      final UUID institutionId, final UUID courseId) {
    final Course course =
        courseRepository
            .findByIdAndInstitution_Id(courseId, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    if (!course.isActive()) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_NOT_ACTIVE);
    }

    final var classes = courseClassRepository.findByCourse_IdOrderByIdAsc(courseId);
    final var classIds = classes.stream().map(value -> value.getId()).toList();
    final var teachers =
        courseClassTeacherRepository.findByCourseClass_IdIn(classIds).stream()
            .collect(Collectors.groupingBy(value -> value.getCourseClass().getId()));
    final var daysByClass =
        courseClassDayRepository.findByCourseClass_IdIn(classIds).stream()
            .collect(Collectors.groupingBy(value -> value.getCourseClass().getId()));
    final var allDays = daysByClass.values().stream().flatMap(List::stream).toList();
    final Map<UUID, List<CourseClassSchedule>> schedulesByDay =
        allDays.isEmpty()
            ? Map.of()
            : courseClassScheduleRepository
                .findByDay_IdIn(allDays.stream().map(value -> value.getId()).toList())
                .stream()
                .collect(Collectors.groupingBy(value -> value.getDay().getId()));
    final Map<UUID, List<CourseIndividualSlot>> slotsBySchedule =
        loadIndividualSlots(course, schedulesByDay);

    final List<CourseEnrollmentClassOptionResponse> classOptions = new ArrayList<>();
    for (final var courseClass : classes) {
      final var days = daysByClass.getOrDefault(courseClass.getId(), List.of());
      final var dayOptions =
          days.stream()
              .map(
                  day ->
                      CourseEnrollmentDayOptionResponse.from(
                          day,
                          schedulesByDay.getOrDefault(day.getId(), List.of()).stream()
                              .map(
                                  schedule ->
                                      toScheduleOption(
                                          schedule,
                                          slotsBySchedule.getOrDefault(
                                              schedule.getId(), List.of())))
                              .toList()))
              .toList();
      final var teacherIds =
          teachers.getOrDefault(courseClass.getId(), List.of()).stream()
              .map(teacher -> teacher.getPerson().getId())
              .toList();
      classOptions.add(
          new CourseEnrollmentClassOptionResponse(courseClass.getId(), teacherIds, dayOptions));
    }

    return new CourseEnrollmentAssignmentOptionsResponse(
        course.getId(), course.getAcademicSpace().getFormat().name(), classOptions);
  }

  private Map<UUID, List<CourseIndividualSlot>> loadIndividualSlots(
      final Course course, final Map<UUID, List<CourseClassSchedule>> schedulesByDay) {
    if (course.getAcademicSpace().getFormat() != AcademicSpaceFormat.INDIVIDUAL) {
      return Map.of();
    }

    final var schedules = schedulesByDay.values().stream().flatMap(List::stream).toList();
    if (schedules.isEmpty()) {
      return Map.of();
    }
    final Map<UUID, List<CourseIndividualSlot>> slotsBySchedule =
        courseIndividualSlotRepository
            .findBySchedule_IdIn(schedules.stream().map(value -> value.getId()).toList())
            .stream()
            .collect(Collectors.groupingBy(value -> value.getSchedule().getId()));
    final List<CourseIndividualSlot> generated = new ArrayList<>();
    for (final var schedule : schedules) {
      if (slotsBySchedule.containsKey(schedule.getId())) {
        continue;
      }

      final var slots = generateSlots(course, schedule);
      slotsBySchedule.put(schedule.getId(), slots);
      generated.addAll(slots);
    }
    if (!generated.isEmpty()) {
      courseIndividualSlotRepository.saveAll(generated);
    }

    return slotsBySchedule;
  }

  private List<CourseIndividualSlot> generateSlots(
      final Course course, final CourseClassSchedule schedule) {
    final Integer durationMinutes = schedule.getDay().getPeriodDurationMinutes();
    final int totalMinutes = schedule.durationMinutes();
    if (durationMinutes == null || durationMinutes <= 0 || totalMinutes % durationMinutes != 0) {
      return List.of();
    }

    final List<CourseIndividualSlot> generated = new ArrayList<>();
    LocalTime start = schedule.getStartTime();
    while (start.isBefore(schedule.getEndTime())) {
      final LocalTime end = start.plusMinutes(durationMinutes);
      generated.add(CourseIndividualSlot.create(course.getInstitution(), schedule, start, end));
      start = end;
    }

    return generated;
  }

  private CourseEnrollmentScheduleOptionResponse toScheduleOption(
      final CourseClassSchedule schedule, final List<CourseIndividualSlot> slots) {
    return CourseEnrollmentScheduleOptionResponse.from(
        schedule, slots.stream().map(CourseIndividualSlotOptionResponse::from).toList());
  }
}
