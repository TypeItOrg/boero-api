package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClass;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassDay;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassSchedule;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceFormat;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseDay;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassDayRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassScheduleRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassTeacherRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.ScopedResource;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AcademicAccessGuard;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AssignPersonSystemRoleUseCase;
import ar.edu.utn.frvm.typeit.boero_api.common.time.BusinessDateProvider;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollmentHistory;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollmentSchedule;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseIndividualSlot;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourseAssignmentSnapshot;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.InstitutionEnrollmentLock;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentSource;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseWithdrawalType;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentHistoryRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentScheduleRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseIndividualSlotRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationCourseAssignmentSnapshotRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationCourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.InstitutionEnrollmentLockRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentHistoryResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentScheduleResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentTeacherOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseScheduleAssignmentRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CreateManualCourseEnrollmentRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollApplicationCourseRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateAcademicEnrollmentStatusRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.WithdrawCourseEnrollmentRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Student;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.StudentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {
  private final AcademicAccessGuard accessGuard;

  private final InstitutionRepository institutionRepository;
  private final AcademicEligibilityService academicEligibilityService;
  private final InstitutionEnrollmentLockRepository institutionEnrollmentLockRepository;
  private final CourseRepository courseRepository;
  private final CourseClassRepository courseClassRepository;
  private final CourseClassTeacherRepository courseClassTeacherRepository;
  private final CourseClassDayRepository courseClassDayRepository;
  private final CourseClassScheduleRepository courseClassScheduleRepository;
  private final CourseIndividualSlotRepository courseIndividualSlotRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final CourseEnrollmentScheduleRepository courseEnrollmentScheduleRepository;
  private final CourseEnrollmentHistoryRepository courseEnrollmentHistoryRepository;
  private final EnrollmentApplicationCourseRepository applicationCourseRepository;
  private final EnrollmentApplicationCourseApprovalService applicationCourseApprovalService;
  private final EnrollmentApplicationCourseAssignmentSnapshotRepository snapshotRepository;
  private final StudentRepository studentRepository;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;
  private final Clock clock;
  private final BusinessDateProvider businessDateProvider;

  @Transactional
  public CourseEnrollmentResponse createManual(
      final UUID institutionId,
      final CreateManualCourseEnrollmentRequest request,
      final UUID authorityPersonId) {
    accessGuard.require(
        PermissionCode.COURSE_ENROLLMENT_CREATE,
        institutionId,
        ScopedResource.COURSE,
        request.courseId());

    final Institution institution = lockInstitution(institutionId);
    final Student student =
        studentRepository
            .findByIdAndInstitution_Id(request.studentId(), institutionId)
            .orElseThrow(
                () -> new EnrollmentValidationException(EnrollmentMessages.PERSON_ID_NOT_FOUND));
    final Course course = findActiveCourse(institutionId, request.courseId());
    final CourseClass courseClass = findCourseClass(institutionId, course, request.courseClassId());
    final var assignments =
        resolveAssignments(institution, course, courseClass, request.assignments());
    academicEligibilityService.requireEligible(institutionId, student.getPerson().getId(), course);
    ensureNoActiveEnrollment(institutionId, student.getId(), course.getId());
    ensureCapacityAndCompatibility(institutionId, student, course, assignments);

    final CourseEnrollment enrollment =
        CourseEnrollment.create(
            institution,
            student,
            course,
            courseClass,
            CourseEnrollmentSource.MANUAL,
            null,
            clock.instant());
    courseEnrollmentRepository.save(enrollment);
    persistAssignments(institution, enrollment, assignments);
    recordHistory(
        institution,
        enrollment,
        null,
        CourseEnrollmentStatus.ENROLLED,
        null,
        AcademicEnrollmentStatus.IN_PROGRESS,
        "MANUAL_ENROLLMENT",
        null,
        authorityPersonId);
    flushEnrollment();
    applicationCourseApprovalService.reevaluateApprovedPendingForCourse(
        institutionId, course.getId());

    return toResponse(enrollment);
  }

  @Transactional
  public CourseEnrollmentResponse enrollApplicationCourse(
      final UUID institutionId,
      final UUID applicationId,
      final UUID applicationCourseId,
      final EnrollApplicationCourseRequest request,
      final UUID authorityPersonId) {
    accessGuard.require(
        PermissionCode.ENROLLMENT_APPLICATION_COURSE_ENROLL,
        institutionId,
        ScopedResource.ENROLLMENT_APPLICATION_COURSE,
        applicationCourseId);

    final Institution institution = lockInstitution(institutionId);
    final EnrollmentApplicationCourse applicationCourse =
        applicationCourseRepository
            .findByIdAndInstitutionIdForUpdate(applicationCourseId, institutionId)
            .orElseThrow(EnrollmentApplicationNotFoundException::new);
    if (!applicationCourse.getEnrollmentApplication().getId().equals(applicationId)) {
      throw new EnrollmentApplicationNotFoundException();
    }
    if (!applicationCourse.getEnrollmentApplication().isApproved()) {
      throw new EnrollmentValidationException(EnrollmentMessages.PARENT_NOT_APPROVED);
    }
    if (applicationCourse.getStatus() == EnrollmentApplicationCourseStatus.ENROLLED) {
      final var existing =
          courseEnrollmentRepository
              .findByInstitution_IdAndApplicationCourse_Id(institutionId, applicationCourse.getId())
              .orElseThrow(EnrollmentApplicationNotFoundException::new);
      return toResponse(existing);
    }
    applicationCourse.ensurePendingResolution();
    ensureExpectedVersion(applicationCourse.getVersion(), request.expectedVersion());

    final Course course = findActiveCourse(institutionId, applicationCourse.getCourse().getId());
    final CourseClass courseClass = findCourseClass(institutionId, course, request.courseClassId());
    final var assignments =
        resolveAssignments(institution, course, courseClass, request.assignments());
    final Student student =
        getOrCreateStudent(
            institution, applicationCourse.getEnrollmentApplication().getApplicantPerson());
    academicEligibilityService.requireEligible(institutionId, student.getPerson().getId(), course);
    ensureNoActiveEnrollment(institutionId, student.getId(), course.getId());
    ensureCapacityAndCompatibility(institutionId, student, course, assignments);

    final CourseEnrollment enrollment =
        CourseEnrollment.create(
            institution,
            student,
            course,
            courseClass,
            CourseEnrollmentSource.APPLICATION,
            applicationCourse.getEnrollmentApplication(),
            clock.instant());
    enrollment.assignApplicationCourse(applicationCourse);
    courseEnrollmentRepository.save(enrollment);
    persistAssignments(institution, enrollment, assignments);
    applicationCourse.enroll(clock.instant(), authorityPersonId);
    applicationCourseRepository.save(applicationCourse);
    persistSnapshots(institution, applicationCourse, courseClass, assignments, authorityPersonId);
    recordHistory(
        institution,
        enrollment,
        null,
        CourseEnrollmentStatus.ENROLLED,
        null,
        AcademicEnrollmentStatus.IN_PROGRESS,
        "APPLICATION_ENROLLMENT",
        null,
        authorityPersonId);
    flushEnrollment();
    applicationCourseApprovalService.reevaluateApprovedPendingForCourse(
        institutionId, course.getId());

    if (student.getPerson() != null) {
      assignPersonSystemRoleUseCase.execute(student.getPerson(), SystemRoleCode.STUDENT, true);
    }

    return toResponse(enrollment);
  }

  @Transactional
  public CourseEnrollmentResponse withdraw(
      final UUID institutionId,
      final UUID enrollmentId,
      final WithdrawCourseEnrollmentRequest request,
      final UUID authorityPersonId) {
    accessGuard.require(
        PermissionCode.COURSE_ENROLLMENT_WITHDRAW,
        institutionId,
        ScopedResource.COURSE_ENROLLMENT,
        enrollmentId);

    final Institution institution = lockInstitution(institutionId);
    final CourseEnrollment enrollment = findEnrollment(institutionId, enrollmentId);
    ensureExpectedVersion(enrollment.getVersion(), request.expectedVersion());
    if (enrollment.getStatus() != CourseEnrollmentStatus.ENROLLED) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ENROLLMENT_NOT_FOUND);
    }

    final CourseEnrollmentStatus target =
        request.type() == CourseWithdrawalType.VOLUNTARY
            ? CourseEnrollmentStatus.WITHDRAWN
            : CourseEnrollmentStatus.ADMINISTRATIVELY_WITHDRAWN;
    final var previousStatus = enrollment.getStatus();
    final var previousAcademicStatus = enrollment.getAcademicStatus();
    enrollment.withdraw(target, clock.instant(), authorityPersonId, request.reason());
    releaseSchedules(enrollment);
    recordHistory(
        institution,
        enrollment,
        previousStatus,
        target,
        previousAcademicStatus,
        enrollment.getAcademicStatus(),
        target.name(),
        request.reason(),
        authorityPersonId);
    flushEnrollment();

    return toResponse(enrollment);
  }

  @Transactional
  public CourseEnrollmentResponse updateAcademicStatus(
      final UUID institutionId,
      final UUID enrollmentId,
      final UpdateAcademicEnrollmentStatusRequest request,
      final UUID authorityPersonId) {
    accessGuard.require(
        PermissionCode.COURSE_ENROLLMENT_ACADEMIC_STATUS_UPDATE,
        institutionId,
        ScopedResource.COURSE_ENROLLMENT,
        enrollmentId);

    final Institution institution = lockInstitution(institutionId);
    final CourseEnrollment enrollment = findEnrollment(institutionId, enrollmentId);
    ensureExpectedVersion(enrollment.getVersion(), request.expectedVersion());
    final var previousStatus = enrollment.getStatus();
    final var previousAcademicStatus = enrollment.getAcademicStatus();
    enrollment.recordAcademicResult(request.status(), clock.instant(), request.reason());
    if (previousStatus == CourseEnrollmentStatus.ENROLLED) {
      releaseSchedules(enrollment);
    }
    recordHistory(
        institution,
        enrollment,
        previousStatus,
        enrollment.getStatus(),
        previousAcademicStatus,
        request.status(),
        "ACADEMIC_STATUS_UPDATE",
        request.reason(),
        authorityPersonId);
    flushEnrollment();

    return toResponse(enrollment);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<CourseEnrollmentResponse> listInstitutional(
      final UUID institutionId,
      final @Nullable CourseEnrollmentStatus status,
      final @Nullable AcademicEnrollmentStatus academicStatus,
      final Pageable pageable) {
    return toPageResponse(
        courseEnrollmentRepository.findByInstitution_Id(
            institutionId, status, academicStatus, pageable));
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<CourseEnrollmentResponse> listOwn(
      final UUID institutionId,
      final UUID personId,
      final @Nullable CourseEnrollmentStatus status,
      final @Nullable AcademicEnrollmentStatus academicStatus,
      final Pageable pageable) {
    return toPageResponse(
        courseEnrollmentRepository.findByInstitutionIdAndStudentPersonId(
            institutionId, personId, status, academicStatus, pageable));
  }

  @Transactional(readOnly = true)
  public CourseEnrollmentResponse get(final UUID institutionId, final UUID enrollmentId) {
    accessGuard.require(
        PermissionCode.COURSE_ENROLLMENT_READ,
        institutionId,
        ScopedResource.COURSE_ENROLLMENT,
        enrollmentId);

    final var enrollment =
        courseEnrollmentRepository
            .findByIdAndInstitutionId(enrollmentId, institutionId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.COURSE_ENROLLMENT_NOT_FOUND));
    return toResponse(enrollment);
  }

  @Transactional(readOnly = true)
  public List<CourseEnrollmentHistoryResponse> history(
      final UUID institutionId, final UUID enrollmentId) {
    accessGuard.require(
        PermissionCode.COURSE_ENROLLMENT_READ,
        institutionId,
        ScopedResource.COURSE_ENROLLMENT,
        enrollmentId);

    courseEnrollmentRepository
        .findByIdAndInstitutionId(enrollmentId, institutionId)
        .orElseThrow(
            () ->
                new EnrollmentValidationException(EnrollmentMessages.COURSE_ENROLLMENT_NOT_FOUND));

    return courseEnrollmentHistoryRepository
        .findByCourseEnrollment_IdOrderByChangedAtDesc(enrollmentId)
        .stream()
        .map(CourseEnrollmentHistoryResponse::from)
        .toList();
  }

  private Institution lockInstitution(final UUID institutionId) {
    final Institution institution =
        institutionRepository
            .findByIdForUpdate(institutionId)
            .orElseThrow(EnrollmentApplicationNotFoundException::new);
    institutionEnrollmentLockRepository
        .findByInstitutionId(institutionId)
        .orElseGet(
            () ->
                institutionEnrollmentLockRepository.save(
                    InstitutionEnrollmentLock.create(institutionId, clock.instant())));
    return institution;
  }

  private Course findActiveCourse(final UUID institutionId, final UUID courseId) {
    final Course course =
        courseRepository
            .findByIdAndInstitution_Id(courseId, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    if (!course.isActive()) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_NOT_ACTIVE);
    }

    return course;
  }

  private CourseClass findCourseClass(
      final UUID institutionId, final Course course, final UUID courseClassId) {
    return courseClassRepository
        .findByIdAndCourseIdAndInstitutionId(courseClassId, course.getId(), institutionId)
        .orElseThrow(
            () -> new EnrollmentValidationException(EnrollmentMessages.COURSE_CLASS_INVALID));
  }

  private List<ResolvedAssignment> resolveAssignments(
      final Institution institution,
      final Course course,
      final CourseClass courseClass,
      final List<CourseScheduleAssignmentRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_REQUIRED);
    }

    final var days = courseClassDayRepository.findByCourseClass_IdIn(List.of(courseClass.getId()));
    final var schedules =
        courseClassScheduleRepository
            .findByDay_IdIn(days.stream().map(CourseClassDay::getId).toList())
            .stream()
            .collect(Collectors.toMap(CourseClassSchedule::getId, value -> value));
    final Set<UUID> selectedDays = new HashSet<>();
    final Set<UUID> selectedSlots = new HashSet<>();
    final List<ResolvedAssignment> assignments = new ArrayList<>();
    final boolean individual =
        course.getAcademicSpace().getFormat() == AcademicSpaceFormat.INDIVIDUAL;

    for (final CourseScheduleAssignmentRequest request : requests) {
      final CourseClassSchedule schedule = schedules.get(request.classScheduleId());
      if (schedule == null) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_INVALID);
      }
      final CourseClassDay day = schedule.getDay();
      if (!selectedDays.add(day.getId())) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_INVALID);
      }
      final CourseIndividualSlot slot =
          individual
              ? resolveIndividualSlot(institution, schedule, request.individualSlotId())
              : null;
      if (individual && !selectedSlots.add(slot.getId())) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_INVALID);
      }
      if (!individual && request.individualSlotId() != null) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_INVALID);
      }
      assignments.add(
          new ResolvedAssignment(
              schedule,
              day,
              slot,
              day.getDayOfWeek(),
              slot == null ? schedule.getStartTime() : slot.getStartTime(),
              slot == null ? schedule.getEndTime() : slot.getEndTime()));
    }

    return assignments;
  }

  private CourseIndividualSlot resolveIndividualSlot(
      final Institution institution,
      final CourseClassSchedule schedule,
      final UUID individualSlotId) {
    if (individualSlotId == null) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_INVALID);
    }
    final var slots =
        courseIndividualSlotRepository.findBySchedule_IdOrderByStartTime(schedule.getId());

    return slots.stream()
        .filter(slot -> slot.getId().equals(individualSlotId))
        .findFirst()
        .orElseThrow(
            () -> new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_INVALID));
  }

  private void ensureNoActiveEnrollment(
      final UUID institutionId, final UUID studentId, final UUID courseId) {
    if (courseEnrollmentRepository
        .findByStudentAndCourseAndStatus(
            institutionId, studentId, courseId, CourseEnrollmentStatus.ENROLLED)
        .isPresent()) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ALREADY_ENROLLED);
    }
  }

  private void ensureCapacityAndCompatibility(
      final UUID institutionId,
      final Student student,
      final Course course,
      final List<ResolvedAssignment> assignments) {
    for (final ResolvedAssignment assignment : assignments) {
      final int activeOccupancy =
          assignment.slot() == null
              ? courseEnrollmentScheduleRepository
                  .findActiveByDay(institutionId, assignment.day().getId())
                  .size()
              : courseEnrollmentScheduleRepository
                  .findActiveByIndividualSlot(institutionId, assignment.slot().getId())
                  .size();
      final Integer capacity = assignment.day().getCapacity();
      if (assignment.slot() != null && activeOccupancy > 0
          || assignment.slot() == null && capacity != null && activeOccupancy >= capacity) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_CAPACITY_EXCEEDED);
      }
    }

    final var existingAssignments =
        courseEnrollmentScheduleRepository.findActiveByStudent(institutionId, student.getId());
    for (final ResolvedAssignment requested : assignments) {
      for (final CourseEnrollmentSchedule existing : existingAssignments) {
        if (!sameAcademicYear(course, existing.getCourseEnrollment().getCourse())
            || existing.getDayOfWeek() != requested.dayOfWeek()
            || !overlaps(
                requested.startTime(),
                requested.endTime(),
                existing.getStartTime(),
                existing.getEndTime())) {
          continue;
        }
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_CONFLICT);
      }
    }
  }

  private static boolean sameAcademicYear(final Course requested, final Course existing) {
    final LocalDate requestedStart = requested.getAcademicYear().getStartDate();
    final LocalDate requestedEnd = requested.getAcademicYear().getEndDate();
    final LocalDate existingStart = existing.getAcademicYear().getStartDate();
    final LocalDate existingEnd = existing.getAcademicYear().getEndDate();
    return requestedStart == null
        || requestedEnd == null
        || existingStart == null
        || existingEnd == null
        || !requestedEnd.isBefore(existingStart) && !existingEnd.isBefore(requestedStart);
  }

  private static boolean overlaps(
      final LocalTime firstStart,
      final LocalTime firstEnd,
      final LocalTime secondStart,
      final LocalTime secondEnd) {
    return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
  }

  private void persistAssignments(
      final Institution institution,
      final CourseEnrollment enrollment,
      final List<ResolvedAssignment> assignments) {
    for (final ResolvedAssignment assignment : assignments) {
      courseEnrollmentScheduleRepository.save(
          CourseEnrollmentSchedule.create(
              institution,
              enrollment,
              assignment.schedule(),
              assignment.slot(),
              assignment.dayOfWeek(),
              assignment.startTime(),
              assignment.endTime()));
    }
  }

  private void persistSnapshots(
      final Institution institution,
      final EnrollmentApplicationCourse applicationCourse,
      final CourseClass courseClass,
      final List<ResolvedAssignment> assignments,
      final UUID authorityPersonId) {
    final Course course = applicationCourse.getCourse();
    final var space = course.getStudyPlanSpace();
    final var level = space.getAcademicLevel();
    final var instrument = course.getInstrument();
    for (final ResolvedAssignment assignment : assignments) {
      snapshotRepository.save(
          EnrollmentApplicationCourseAssignmentSnapshot.builder()
              .institution(institution)
              .applicationCourse(applicationCourse)
              .course(course)
              .courseClass(courseClass)
              .schedule(assignment.schedule())
              .individualSlotId(assignment.slot() == null ? null : assignment.slot().getId())
              .courseName(space.getAcademicSpace().getName())
              .trainingPathName(space.getStudyPlan().getTrainingPath().getName())
              .studyPlanName(space.getStudyPlan().getName())
              .academicSpaceName(space.getAcademicSpace().getName())
              .academicLevelName(level == null ? null : level.getName())
              .instrumentName(instrument == null ? null : instrument.getName())
              .dayOfWeek(assignment.dayOfWeek())
              .startTime(assignment.startTime())
              .endTime(assignment.endTime())
              .assignedAt(clock.instant())
              .assignedByPersonId(authorityPersonId)
              .operation("APPLICATION_ENROLLMENT")
              .build());
    }
  }

  private Student getOrCreateStudent(final Institution institution, final Person person) {
    return studentRepository
        .findByInstitution_IdAndPerson_Id(institution.getId(), person.getId())
        .orElseGet(
            () ->
                studentRepository.save(
                    Student.builder()
                        .institution(institution)
                        .person(person)
                        .fileNumber(
                            String.format(
                                "%d-%05d",
                                businessDateProvider.today().getYear(),
                                studentRepository.nextFileNumberSequenceValue()))
                        .enrollmentDate(businessDateProvider.today())
                        .build()));
  }

  private CourseEnrollment findEnrollment(final UUID institutionId, final UUID enrollmentId) {
    return courseEnrollmentRepository
        .findByIdAndInstitutionIdForUpdate(enrollmentId, institutionId)
        .orElseThrow(
            () ->
                new EnrollmentValidationException(EnrollmentMessages.COURSE_ENROLLMENT_NOT_FOUND));
  }

  private void releaseSchedules(final CourseEnrollment enrollment) {
    for (final CourseEnrollmentSchedule schedule :
        courseEnrollmentScheduleRepository.findByCourseEnrollment_Id(enrollment.getId())) {
      if (schedule.getReleasedAt() == null) {
        schedule.release(clock.instant());
      }
    }
  }

  private void recordHistory(
      final Institution institution,
      final CourseEnrollment enrollment,
      final CourseEnrollmentStatus previousStatus,
      final CourseEnrollmentStatus newStatus,
      final AcademicEnrollmentStatus previousAcademicStatus,
      final AcademicEnrollmentStatus newAcademicStatus,
      final String operation,
      final String reason,
      final UUID authorityPersonId) {
    courseEnrollmentHistoryRepository.save(
        CourseEnrollmentHistory.create(
            institution,
            enrollment,
            previousStatus,
            newStatus,
            previousAcademicStatus,
            newAcademicStatus,
            operation,
            reason,
            authorityPersonId,
            clock.instant()));
  }

  private void ensureExpectedVersion(final long actualVersion, final Long expectedVersion) {
    if (expectedVersion != null && expectedVersion != actualVersion) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ENROLLMENT_VERSION_STALE);
    }
  }

  private void flushEnrollment() {
    try {
      courseEnrollmentRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      final String constraintName = constraintName(exception);

      if ("course_enrollment_academic_requirements_check".equals(constraintName)) {
        throw new EnrollmentValidationException(EnrollmentMessages.ACADEMIC_REQUIREMENTS_PENDING);
      }
      if ("course_enrollment_schedules_active_slot_unique".equals(constraintName)
          || "course_enrollment_schedules_capacity_check".equals(constraintName)) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_CAPACITY_EXCEEDED);
      }
      if ("course_enrollments_active_student_course_unique".equals(constraintName)
          || "course_enrollments_application_course_unique".equals(constraintName)) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ALREADY_ENROLLED);
      }

      if ("course_enrollment_schedules_context_check".equals(constraintName)
          || "course_enrollment_schedules_active_day_unique".equals(constraintName)
          || "course_enrollment_schedules_group_assignment_unique".equals(constraintName)
          || "course_enrollment_schedules_matching_slot_fk".equals(constraintName)) {
        throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ASSIGNMENT_INVALID);
      }
      if ("course_enrollments_origin_check".equals(constraintName)) {
        throw new EnrollmentValidationException(EnrollmentMessages.PARENT_NOT_APPROVED);
      }
      throw exception;
    }
  }

  private String constraintName(final DataIntegrityViolationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        return violation.getConstraintName();
      }
    }

    return null;
  }

  private CourseEnrollmentResponse toResponse(final CourseEnrollment enrollment) {
    final List<CourseEnrollmentScheduleResponse> schedules =
        courseEnrollmentScheduleRepository.findByCourseEnrollment_Id(enrollment.getId()).stream()
            .map(CourseEnrollmentScheduleResponse::from)
            .toList();
    final var teachers =
        courseClassTeacherRepository
            .findByCourseClass_IdIn(List.of(enrollment.getCourseClass().getId()))
            .stream()
            .map(assignment -> CourseEnrollmentTeacherOptionResponse.from(assignment.getPerson()))
            .toList();

    return CourseEnrollmentResponse.from(enrollment, schedules, teachers);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<CourseEnrollmentResponse> listForTeacherClass(
      final UUID institutionId, final UUID personId, final UUID classId, final Pageable pageable) {
    return toPageResponse(
        courseEnrollmentRepository.findForTeacherClass(institutionId, personId, classId, pageable));
  }

  private PaginatedResponse<CourseEnrollmentResponse> toPageResponse(
      final Page<CourseEnrollment> enrollments) {
    final List<UUID> enrollmentIds =
        enrollments.getContent().stream().map(CourseEnrollment::getId).toList();
    if (enrollmentIds.isEmpty()) {
      return PaginatedResponse.from(
          enrollments.map(enrollment -> CourseEnrollmentResponse.from(enrollment, List.of())));
    }

    final var classIds =
        enrollments.getContent().stream()
            .map(enrollment -> enrollment.getCourseClass().getId())
            .distinct()
            .toList();
    final var teachersByClass =
        courseClassTeacherRepository.findByCourseClass_IdIn(classIds).stream()
            .collect(
                Collectors.groupingBy(
                    assignment -> assignment.getCourseClass().getId(),
                    Collectors.mapping(
                        assignment ->
                            CourseEnrollmentTeacherOptionResponse.from(assignment.getPerson()),
                        Collectors.toList())));

    final Map<UUID, List<CourseEnrollmentScheduleResponse>> schedulesByEnrollment =
        courseEnrollmentScheduleRepository.findByCourseEnrollment_IdIn(enrollmentIds).stream()
            .collect(
                Collectors.groupingBy(
                    schedule -> schedule.getCourseEnrollment().getId(),
                    Collectors.mapping(
                        CourseEnrollmentScheduleResponse::from, Collectors.toList())));
    return PaginatedResponse.from(
        enrollments.map(
            enrollment ->
                CourseEnrollmentResponse.from(
                    enrollment,
                    schedulesByEnrollment.getOrDefault(enrollment.getId(), List.of()),
                    teachersByClass.getOrDefault(enrollment.getCourseClass().getId(), List.of()))));
  }

  private record ResolvedAssignment(
      CourseClassSchedule schedule,
      CourseClassDay day,
      CourseIndividualSlot slot,
      CourseDay dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {}
}
