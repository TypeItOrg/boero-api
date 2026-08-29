package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLifecycleEvent;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicLifecycleAction;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicLifecycleResource;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.InstrumentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLifecycleEventRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicLifecycleRequest;
import ar.edu.utn.frvm.typeit.boero_api.common.logging.RequestLoggingFilter;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicLifecycleService {

  private final AcademicYearRepository academicYearRepository;
  private final TrainingPathRepository trainingPathRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final AcademicSpaceRepository academicSpaceRepository;
  private final InstrumentRepository instrumentRepository;
  private final CourseRepository courseRepository;
  private final AcademicLifecycleEventRepository eventRepository;
  private final AcademicLifecycleActorResolver actorResolver;

  @Transactional
  public void deleteAcademicYear(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var year =
        academicYearRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    if (year.delete(LocalDateTime.now())) {
      record(
          year.getInstitution(),
          AcademicLifecycleResource.ACADEMIC_YEAR,
          id,
          AcademicLifecycleAction.DELETE,
          request);
    }
  }

  @Transactional
  public void restoreAcademicYear(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var year =
        academicYearRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    restore(year, year.getInstitution(), AcademicLifecycleResource.ACADEMIC_YEAR, id, request);
  }

  @Transactional
  public void deleteTrainingPath(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var path =
        trainingPathRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(TrainingPathNotFoundException::new);
    if (!path.isDeleted() && trainingPathRepository.existsCurrentStudyPlan(id)) {
      throw new AcademicConflictException(AcademicMessages.DELETE_REFERENCED_RESOURCE);
    }
    if (path.delete(LocalDateTime.now())) {
      record(
          path.getInstitution(),
          AcademicLifecycleResource.TRAINING_PATH,
          id,
          AcademicLifecycleAction.DELETE,
          request);
    }
  }

  @Transactional
  public void restoreTrainingPath(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var path =
        trainingPathRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(TrainingPathNotFoundException::new);
    restore(path, path.getInstitution(), AcademicLifecycleResource.TRAINING_PATH, id, request);
  }

  @Transactional
  public void deleteStudyPlan(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var plan =
        studyPlanRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    if (plan.delete(LocalDateTime.now())) {
      record(
          plan.getInstitution(),
          AcademicLifecycleResource.STUDY_PLAN,
          id,
          AcademicLifecycleAction.DELETE,
          request);
    }
  }

  @Transactional
  public void restoreStudyPlan(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var plan =
        studyPlanRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    if (plan.isDeleted()
        && (plan.getTrainingPath().isDeleted() || !plan.getTrainingPath().isActive())) {
      throw new AcademicConflictException(AcademicMessages.RESTORE_PARENT_UNAVAILABLE);
    }
    restore(plan, plan.getInstitution(), AcademicLifecycleResource.STUDY_PLAN, id, request);
  }

  @Transactional
  public void deleteAcademicSpace(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    if (space.delete(LocalDateTime.now())) {
      record(
          space.getInstitution(),
          AcademicLifecycleResource.ACADEMIC_SPACE,
          id,
          AcademicLifecycleAction.DELETE,
          request);
    }
  }

  @Transactional
  public void restoreAcademicSpace(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    restore(space, space.getInstitution(), AcademicLifecycleResource.ACADEMIC_SPACE, id, request);
  }

  @Transactional
  public void deleteInstrument(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var instrument =
        instrumentRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(InstrumentNotFoundException::new);
    if (instrument.delete(LocalDateTime.now())) {
      record(
          instrument.getInstitution(),
          AcademicLifecycleResource.INSTRUMENT,
          id,
          AcademicLifecycleAction.DELETE,
          request);
    }
  }

  @Transactional
  public void restoreInstrument(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var instrument =
        instrumentRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(InstrumentNotFoundException::new);
    restore(
        instrument, instrument.getInstitution(), AcademicLifecycleResource.INSTRUMENT, id, request);
  }

  @Transactional
  public void deleteCourse(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    lockCourseParents(institutionId, id);
    final var course =
        courseRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    if (course.delete(LocalDateTime.now())) {
      record(
          course.getInstitution(),
          AcademicLifecycleResource.COURSE,
          id,
          AcademicLifecycleAction.DELETE,
          request);
    }
  }

  @Transactional
  public void restoreCourse(
      final UUID institutionId, final UUID id, final AcademicLifecycleRequest request) {
    final var parents = lockCourseParents(institutionId, id);
    final var course =
        courseRepository
            .findByIdAndInstitution_IdForLifecycle(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    final boolean parentUnavailable =
        course.isDeleted()
            && (parents.studyPlan().getStatus() != StudyPlanStatus.ACTIVE
                || parents.academicYear().getStatus() != AcademicYearStatus.ACTIVE);
    if (parentUnavailable) {
      throw new AcademicConflictException(AcademicMessages.RESTORE_PARENT_UNAVAILABLE);
    }
    restore(course, course.getInstitution(), AcademicLifecycleResource.COURSE, id, request);
  }

  private CourseLifecycleParents lockCourseParents(final UUID institutionId, final UUID courseId) {
    final var context =
        courseRepository
            .findAcademicContextByIdAndInstitution_Id(courseId, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    final var studyPlan =
        studyPlanRepository
            .findByIdAndInstitution_IdForLifecycle(context.getStudyPlanId(), institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    final var academicYear =
        academicYearRepository
            .findByIdAndInstitution_IdForUpdate(context.getAcademicYearId(), institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    return new CourseLifecycleParents(studyPlan, academicYear);
  }

  private record CourseLifecycleParents(StudyPlan studyPlan, AcademicYear academicYear) {}

  private void restore(
      final SoftDeletable resource,
      final Institution institution,
      final AcademicLifecycleResource resourceType,
      final UUID resourceId,
      final AcademicLifecycleRequest request) {
    if (!resource.restore()) {
      return;
    }
    record(institution, resourceType, resourceId, AcademicLifecycleAction.RESTORE, request);
  }

  private void record(
      final Institution institution,
      final AcademicLifecycleResource resourceType,
      final UUID resourceId,
      final AcademicLifecycleAction action,
      final AcademicLifecycleRequest request) {
    final var actor = actorResolver.resolve();
    final var now = LocalDateTime.now();
    try {
      eventRepository.save(
          AcademicLifecycleEvent.create(
              institution,
              resourceType,
              resourceId,
              action,
              actor.type(),
              actor.id(),
              normalizeReason(request),
              MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY),
              now));
      eventRepository.flush();
      log.info(
          "Academic lifecycle transition, resourceType: {}, resourceId: {}, action: {}, actorType: {}, actorId: {}",
          resourceType,
          resourceId,
          action,
          actor.type(),
          actor.id());
    } catch (DataIntegrityViolationException exception) {
      if (action == AcademicLifecycleAction.RESTORE) {
        throw new AcademicConflictException(AcademicMessages.RESTORE_CONFLICT);
      }
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }

  private static String normalizeReason(final AcademicLifecycleRequest request) {
    if (request == null || request.reason() == null || request.reason().isBlank()) {
      return null;
    }
    return request.reason().trim();
  }
}
