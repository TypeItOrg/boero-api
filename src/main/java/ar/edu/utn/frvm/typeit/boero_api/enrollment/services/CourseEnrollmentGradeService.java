package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClass;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassTeacherRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollmentGrade;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentGradePublicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.GradeMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentGradeRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.InstitutionEnrollmentLockRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.InstitutionEnrollmentLock;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentGradeResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentGradeStudentResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PendingClassGradesResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PendingGradesSummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PublishGradesResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEnrollmentGradeService {

  private final InstitutionRepository institutionRepository;
  private final InstitutionEnrollmentLockRepository institutionEnrollmentLockRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final CourseEnrollmentGradeRepository gradeRepository;
  private final CourseClassRepository courseClassRepository;
  private final CourseClassTeacherRepository courseClassTeacherRepository;
  private final PersonRoleAssignmentRepository roleAssignments;
  private final PersonRepository personRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<CourseEnrollmentGradeResponse> listForManagement(
      final UUID institutionId, final UUID enrollmentId) {
    final CourseEnrollment enrollment = findEnrollment(institutionId, enrollmentId);

    final List<CourseEnrollmentGrade> grades =
        gradeRepository.findByEnrollment(institutionId, enrollment.getId());

    return toManagementResponses(grades);
  }

  @Transactional(readOnly = true)
  public List<CourseEnrollmentGradeStudentResponse> listPublished(
      final UUID institutionId, final UUID enrollmentId) {
    final CourseEnrollment enrollment = findEnrollmentReadOnly(institutionId, enrollmentId);

    return gradeRepository.findPublishedByEnrollment(institutionId, enrollment.getId()).stream()
        .map(CourseEnrollmentGradeStudentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CourseEnrollmentGradeStudentResponse> listOwnPublished(
      final UUID institutionId, final UUID enrollmentId, final UUID personId) {
    final CourseEnrollment enrollment = findEnrollmentReadOnly(institutionId, enrollmentId);

    if (!enrollment.getStudent().getPerson().getId().equals(personId)) {
      throw new AccessDeniedException(SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE);
    }

    return gradeRepository.findPublishedByEnrollment(institutionId, enrollment.getId()).stream()
        .map(CourseEnrollmentGradeStudentResponse::from)
        .toList();
  }

  @Transactional
  public CourseEnrollmentGradeResponse create(
      final UUID institutionId,
      final UUID enrollmentId,
      final String evaluation,
      final BigDecimal value,
      final @Nullable UUID actorPersonId) {
    lockInstitution(institutionId);

    final CourseEnrollment enrollment = findEnrollmentForUpdate(institutionId, enrollmentId);
    requireEnrolled(enrollment);

    final CourseEnrollmentGrade grade =
        CourseEnrollmentGrade.create(
            enrollment.getInstitution(), enrollment, evaluation, value, actorPersonId);

    if (gradeRepository.existsDuplicateEvaluation(
        enrollment.getId(), grade.getEvaluation(), null)) {
      throw new EnrollmentValidationException(GradeMessages.EVALUATION_DUPLICATE);
    }

    try {
      gradeRepository.saveAndFlush(grade);
    } catch (final DataIntegrityViolationException exception) {
      throw translateIntegrityViolation(exception);
    }

    log.info(
        "[Grades] Created draft, institution: {}, enrollment: {}, grade: {}",
        institutionId,
        enrollmentId,
        grade.getId());

    return toManagementResponses(List.of(grade)).get(0);
  }

  @Transactional
  public CourseEnrollmentGradeResponse update(
      final UUID institutionId,
      final UUID enrollmentId,
      final UUID gradeId,
      final String evaluation,
      final BigDecimal value,
      final @Nullable Long expectedVersion,
      final @Nullable UUID actorPersonId) {
    lockInstitution(institutionId);

    final CourseEnrollment enrollment = findEnrollmentForUpdate(institutionId, enrollmentId);
    requireEnrolled(enrollment);

    final CourseEnrollmentGrade grade = findGradeForUpdate(institutionId, gradeId);
    requireGradeBelongsToEnrollment(grade, enrollment);
    ensureExpectedVersion(grade.getVersion(), expectedVersion);

    final String normalizedEvaluation = CourseEnrollmentGrade.normalizeEvaluation(evaluation);
    CourseEnrollmentGrade.normalizeValue(value);

    if (gradeRepository.existsDuplicateEvaluation(
        enrollment.getId(), normalizedEvaluation, grade.getId())) {
      throw new EnrollmentValidationException(GradeMessages.EVALUATION_DUPLICATE);
    }

    grade.updateWorkingCopy(evaluation, value, actorPersonId);

    try {
      gradeRepository.flush();
    } catch (final DataIntegrityViolationException exception) {
      throw translateIntegrityViolation(exception);
    }

    return toManagementResponses(List.of(grade)).get(0);
  }

  @Transactional
  public DeleteResult delete(
      final UUID institutionId,
      final UUID enrollmentId,
      final UUID gradeId,
      final @Nullable Long expectedVersion,
      final @Nullable UUID actorPersonId) {
    lockInstitution(institutionId);

    final CourseEnrollment enrollment = findEnrollmentForUpdate(institutionId, enrollmentId);
    requireEnrolled(enrollment);

    final CourseEnrollmentGrade grade = findGradeForUpdate(institutionId, gradeId);
    requireGradeBelongsToEnrollment(grade, enrollment);
    ensureExpectedVersion(grade.getVersion(), expectedVersion);

    if (grade.isDraft()) {
      gradeRepository.delete(grade);
      gradeRepository.flush();

      return new DeleteResult(true, null);
    }

    grade.markForDeletion(actorPersonId);
    gradeRepository.flush();

    return new DeleteResult(false, toManagementResponses(List.of(grade)).get(0));
  }

  @Transactional
  public CourseEnrollmentGradeResponse cancelDeletion(
      final UUID institutionId,
      final UUID enrollmentId,
      final UUID gradeId,
      final @Nullable UUID actorPersonId) {
    lockInstitution(institutionId);

    final CourseEnrollment enrollment = findEnrollmentForUpdate(institutionId, enrollmentId);
    requireEnrolled(enrollment);

    final CourseEnrollmentGrade grade = findGradeForUpdate(institutionId, gradeId);
    requireGradeBelongsToEnrollment(grade, enrollment);

    grade.cancelDeletion(actorPersonId);
    gradeRepository.flush();

    return toManagementResponses(List.of(grade)).get(0);
  }

  @Transactional(readOnly = true)
  public PendingGradesSummaryResponse pendingSummary(
      final UUID institutionId, final UUID classId) {
    final CourseClass courseClass = findClassReadOnly(institutionId, classId);

    final List<CourseEnrollmentGrade> pending =
        gradeRepository.findPendingByClass(institutionId, courseClass.getId());

    final Set<UUID> students = new HashSet<>();
    long created = 0;
    long modified = 0;
    long deleted = 0;

    for (final CourseEnrollmentGrade grade : pending) {
      students.add(grade.getCourseEnrollment().getId());

      final var status = grade.publicationStatus();

      if (status == CourseEnrollmentGradePublicationStatus.PENDING_DELETION) {
        deleted++;
      } else if (status == CourseEnrollmentGradePublicationStatus.DRAFT) {
        created++;
      } else {
        modified++;
      }
    }

    return new PendingGradesSummaryResponse(
        pending.size(), students.size(), created, modified, deleted);
  }

  @Transactional(readOnly = true)
  public List<PendingClassGradesResponse> pendingClasses(final UUID institutionId) {
    final List<CourseEnrollmentGrade> pending = gradeRepository.findPendingDetails(institutionId);

    final Map<UUID, List<CourseEnrollmentGrade>> byClass =
        pending.stream()
            .collect(Collectors.groupingBy(grade -> grade.getCourseEnrollment().getCourseClass().getId()));

    return byClass.values().stream()
        .map(
            classGrades -> {
              final var first = classGrades.get(0);
              final var courseClass = first.getCourseEnrollment().getCourseClass();
              final var course = courseClass.getCourse();
              final Set<UUID> students = new HashSet<>();

              for (final CourseEnrollmentGrade grade : classGrades) {
                students.add(grade.getCourseEnrollment().getId());
              }

              return new PendingClassGradesResponse(
                  course.getId(),
                  course.getAcademicSpace().getName(),
                  courseClass.getId(),
                  courseClass.displayName(),
                  classGrades.size(),
                  students.size());
            })
        .sorted(
            (left, right) -> {
              final int byCourse = left.courseName().compareToIgnoreCase(right.courseName());
              return byCourse != 0 ? byCourse : left.classLabel().compareToIgnoreCase(right.classLabel());
            })
        .toList();
  }

  @Transactional
  public PublishGradesResponse publishClass(
      final UUID institutionId, final UUID classId, final UUID publisherPersonId) {
    lockInstitution(institutionId);

    final CourseClass courseClass = findClassForUpdate(institutionId, classId);

    final List<CourseEnrollmentGrade> grades =
        gradeRepository.findByClassForUpdate(institutionId, courseClass.getId());

    final List<CourseEnrollmentGrade> pending =
        grades.stream().filter(CourseEnrollmentGrade::hasPendingChanges).toList();

    if (pending.isEmpty()) {
      throw new EnrollmentValidationException(GradeMessages.PUBLISH_NO_CHANGES);
    }

    final Instant now = clock.instant();
    final Set<UUID> affectedEnrollments = new HashSet<>();
    long published = 0;
    long deleted = 0;

    for (final CourseEnrollmentGrade grade : pending) {
      affectedEnrollments.add(grade.getCourseEnrollment().getId());

      if (grade.publicationStatus() == CourseEnrollmentGradePublicationStatus.PENDING_DELETION) {
        gradeRepository.delete(grade);
        deleted++;
        continue;
      }

      grade.publish(publisherPersonId, now);
      published++;
    }

    try {
      gradeRepository.flush();
    } catch (final DataIntegrityViolationException exception) {
      throw translateIntegrityViolation(exception);
    }

    log.info(
        "[Grades] Published class, institution: {}, class: {}, published: {}, deleted: {}",
        institutionId,
        classId,
        published,
        deleted);

    return new PublishGradesResponse(published, deleted, affectedEnrollments.size());
  }

  public void requireTeacherForClass(
      final UUID institutionId, final UUID personId, final UUID classId) {
    if (!roleAssignments.existsByPerson_IdAndInstitution_IdAndRole_Code(
        personId, institutionId, SystemRoleCode.TEACHER.name())) {
      throw new AccessDeniedException(SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE);
    }

    if (!courseClassTeacherRepository.existsByInstitution_IdAndPerson_IdAndCourseClass_Id(
        institutionId, personId, classId)) {
      throw new EnrollmentValidationException(GradeMessages.TEACHER_NOT_ASSIGNED);
    }
  }

  public void requireEnrollmentInClass(
      final UUID institutionId, final UUID classId, final UUID enrollmentId) {
    final CourseEnrollment enrollment = findEnrollmentReadOnly(institutionId, enrollmentId);

    if (!enrollment.getCourseClass().getId().equals(classId)) {
      throw new EnrollmentValidationException(GradeMessages.GRADE_CLASS_MISMATCH);
    }
  }

  private Institution lockInstitution(final UUID institutionId) {
    final Institution institution =
        institutionRepository
            .findByIdForUpdate(institutionId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.COURSE_ENROLLMENT_NOT_FOUND));
    institutionEnrollmentLockRepository
        .findByInstitutionId(institutionId)
        .orElseGet(
            () ->
                institutionEnrollmentLockRepository.save(
                    InstitutionEnrollmentLock.create(institutionId, clock.instant())));

    return institution;
  }

  private CourseEnrollment findEnrollment(final UUID institutionId, final UUID enrollmentId) {
    return courseEnrollmentRepository
        .findByIdAndInstitutionId(enrollmentId, institutionId)
        .orElseThrow(
            () ->
                new EnrollmentValidationException(
                    EnrollmentMessages.COURSE_ENROLLMENT_NOT_FOUND));
  }

  private CourseEnrollment findEnrollmentReadOnly(
      final UUID institutionId, final UUID enrollmentId) {
    return findEnrollment(institutionId, enrollmentId);
  }

  private CourseEnrollment findEnrollmentForUpdate(
      final UUID institutionId, final UUID enrollmentId) {
    return courseEnrollmentRepository
        .findByIdAndInstitutionIdForUpdate(enrollmentId, institutionId)
        .orElseThrow(
            () ->
                new EnrollmentValidationException(
                    EnrollmentMessages.COURSE_ENROLLMENT_NOT_FOUND));
  }

  private CourseEnrollmentGrade findGradeForUpdate(final UUID institutionId, final UUID gradeId) {
    return gradeRepository
        .findByIdAndInstitutionIdForUpdate(gradeId, institutionId)
        .orElseThrow(() -> new EnrollmentValidationException(GradeMessages.GRADE_NOT_FOUND));
  }

  private CourseClass findClassReadOnly(final UUID institutionId, final UUID classId) {
    return courseClassRepository
        .findByIdAndInstitutionId(classId, institutionId)
        .orElseThrow(
            () -> new EnrollmentValidationException(EnrollmentMessages.COURSE_CLASS_INVALID));
  }

  private CourseClass findClassForUpdate(final UUID institutionId, final UUID classId) {
    return courseClassRepository
        .findByIdAndInstitutionIdForUpdate(classId, institutionId)
        .orElseThrow(
            () -> new EnrollmentValidationException(EnrollmentMessages.COURSE_CLASS_INVALID));
  }

  private void requireEnrolled(final CourseEnrollment enrollment) {
    if (enrollment.getStatus() != CourseEnrollmentStatus.ENROLLED) {
      throw new EnrollmentValidationException(GradeMessages.ENROLLMENT_NOT_ENROLLED);
    }
  }

  private void requireGradeBelongsToEnrollment(
      final CourseEnrollmentGrade grade, final CourseEnrollment enrollment) {
    if (!grade.getCourseEnrollment().getId().equals(enrollment.getId())) {
      throw new EnrollmentValidationException(GradeMessages.GRADE_ENROLLMENT_MISMATCH);
    }
  }

  private void ensureExpectedVersion(
      final long actualVersion, final @Nullable Long expectedVersion) {
    if (expectedVersion != null && expectedVersion != actualVersion) {
      throw new EnrollmentValidationException(GradeMessages.GRADE_VERSION_STALE);
    }
  }

  private List<CourseEnrollmentGradeResponse> toManagementResponses(
      final List<CourseEnrollmentGrade> grades) {
    final Set<UUID> personIds = new HashSet<>();

    for (final CourseEnrollmentGrade grade : grades) {
      if (grade.getCreatedByPersonId() != null) {
        personIds.add(grade.getCreatedByPersonId());
      }

      if (grade.getUpdatedByPersonId() != null) {
        personIds.add(grade.getUpdatedByPersonId());
      }

      if (grade.getPublishedByPersonId() != null) {
        personIds.add(grade.getPublishedByPersonId());
      }
    }

    final Map<UUID, Person> people =
        personIds.isEmpty()
            ? Map.of()
            : personRepository.findAllById(personIds).stream()
                .collect(Collectors.toMap(Person::getId, person -> person));

    return grades.stream()
        .map(grade -> CourseEnrollmentGradeResponse.from(grade, people))
        .toList();
  }

  private RuntimeException translateIntegrityViolation(
      final DataIntegrityViolationException exception) {
    final String constraint = constraintName(exception);

    if ("course_enrollment_grades_enrollment_evaluation_unique".equals(constraint)) {
      return new EnrollmentValidationException(GradeMessages.EVALUATION_DUPLICATE);
    }

    if (constraint != null && constraint.contains("course_enrollment_grades_value_range_check")) {
      return new EnrollmentValidationException(GradeMessages.VALUE_OUT_OF_RANGE);
    }

    if (constraint != null
        && constraint.contains("course_enrollment_grades_published_value_range_check")) {
      return new EnrollmentValidationException(GradeMessages.VALUE_OUT_OF_RANGE);
    }

    return exception;
  }

  private @Nullable String constraintName(final DataIntegrityViolationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        return violation.getConstraintName();
      }
    }

    return null;
  }

  public record DeleteResult(
      boolean deletedImmediately, @Nullable CourseEnrollmentGradeResponse grade) {}
}
