package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassTeacherRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import ar.edu.utn.frvm.typeit.boero_api.common.time.BusinessDateProvider;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantEducationBackground;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantHealthInclusion;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantPreference;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantResponsible;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationSpace;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ActiveEnrollmentApplicationExistsException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationCourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AcademicBackgroundDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseSelectionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.HealthInclusionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PreferenceDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.ResponsibleDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.StudentRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentApplicationService {

  private final EnrollmentApplicationRepository applicationRepository;
  private final EnrollmentPeriodRepository periodRepository;
  private final PersonRepository personRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final TrainingPathRepository trainingPathRepository;
  private final CourseRepository courseRepository;
  private final CourseClassTeacherRepository courseClassTeacherRepository;
  private final EnrollmentApplicationCourseRepository applicationCourseRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final StudentRepository studentRepository;
  private final AcademicYearRepository academicYearRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final InstrumentRepository instrumentRepository;
  private final EnrollmentDraftDataValidator enrollmentDraftDataValidator;
  private final EnrollmentApplicationCourseApprovalService applicationCourseApprovalService;
  private final BusinessDateProvider businessDateProvider;
  private final Clock clock;
  private final EnrollmentInstitutionLock enrollmentInstitutionLock;

  @Transactional
  public EnrollmentApplicationResponse startOrGetApplication(
      UUID institutionId, UUID personId, StartEnrollmentApplicationRequest request) {

    enrollmentInstitutionLock.lock(institutionId);

    Person person =
        personRepository
            .findByIdAndInstitution_Id(personId, institutionId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.ENROLLMENT_APPLICATION_APPLICANT_REQUIRED));
    if (request.getTrainingPathId() != null) {
      return startByTrainingPath(institutionId, personId, person, request);
    }

    throw new EnrollmentValidationException(EnrollmentMessages.COURSE_SELECTION_REQUIRED);
  }

  private EnrollmentApplicationResponse startByTrainingPath(
      final UUID institutionId,
      final UUID personId,
      final Person person,
      final StartEnrollmentApplicationRequest request) {
    final TrainingPath trainingPath =
        trainingPathRepository
            .findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(
                request.getTrainingPathId(), institutionId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID));
    if (!courseRepository.existsActiveByTrainingPath(institutionId, trainingPath.getId())) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID);
    }

    final AcademicYear academicYear =
        resolveAcademicYear(institutionId, request.getAcademicYearId());
    final var openApplications =
        applicationRepository.findOpenByApplicantAndContext(
            personId, institutionId, trainingPath.getId(), academicYear.getId());
    final var draft =
        openApplications.stream()
            .filter(application -> application.getStatus() == EnrollmentApplicationStatus.DRAFT)
            .findFirst();
    if (draft.isPresent()) {
      return EnrollmentApplicationResponse.from(draft.get());
    }
    // A SUBMITTED application no longer blocks a new draft for the same training
    // path: blocking applies per course (requested selections and active enrollments)
    // when courses are chosen or submitted, so applicants are never locked out for
    // forgetting a course.

    final EnrollmentPeriod period =
        periodRepository
            .findActivePeriod(
                institutionId, academicYear.getId(), EnrollmentPeriodStatus.OPEN, clock.instant())
            .orElseThrow(EnrollmentPeriodClosedException::new);
    final EnrollmentApplication application =
        EnrollmentApplication.createForTrainingPath(
            period.getInstitution(), person, trainingPath, academicYear, period);

    return EnrollmentApplicationResponse.from(saveAndFlush(application));
  }

  private AcademicYear resolveAcademicYear(
      final UUID institutionId, final @Nullable UUID academicYearId) {
    if (academicYearId != null) {
      return academicYearRepository
          .findByIdAndInstitution_Id(academicYearId, institutionId)
          .orElseThrow(
              () ->
                  new EnrollmentValidationException(
                      EnrollmentMessages.ACADEMIC_YEAR_ID_NOT_FOUND + academicYearId));
    }

    final var activeYears =
        academicYearRepository.findAllByInstitutionIdAndStatus(
            institutionId, AcademicYearStatus.ACTIVE);
    if (activeYears.isEmpty()) {
      throw new EnrollmentValidationException(EnrollmentMessages.ACADEMIC_YEAR_ID_NOT_FOUND);
    }
    if (activeYears.size() > 1) {
      throw new EnrollmentValidationException(EnrollmentMessages.ACADEMIC_YEAR_AMBIGUOUS);
    }
    return activeYears.getFirst();
  }

  @Transactional
  public EnrollmentApplicationResponse updateDraft(
      UUID personId, UUID applicationId, UpdateEnrollmentDraftRequest request) {
    final var institutionId =
        applicationRepository
            .findOwnedInstitutionId(applicationId, personId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));
    enrollmentInstitutionLock.lock(institutionId);
    EnrollmentApplication application =
        applicationRepository
            .findOwnedForUpdate(applicationId, personId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    if (request.getData() != null) {
      EnrollmentDraftData data = request.getData();

      // 1. Los datos personales se leen de Person (padrón institucional) y no
      // se editan desde el borrador: escribirlos aquí sin validación ni chequeo
      // de unicidad permitía pisar documentNumber/email del registro institucional.

      // 2. Antecedentes académicos
      AcademicBackgroundDto academicBg = data.getAcademicBackground();

      if (academicBg != null) {
        ApplicantEducationBackground bg = application.getEducationBackground();

        if (bg == null) {
          bg = ApplicantEducationBackground.builder().enrollmentApplication(application).build();
          application.setEducationBackground(bg);
        }

        if (academicBg.getSecondarySchool() != null) {
          bg.setSecondarySchool(academicBg.getSecondarySchool());
        }

        if (academicBg.getSchoolOrigin() != null) {
          bg.setSchoolOrigin(academicBg.getSchoolOrigin());
        }

        if (academicBg.getCurrentGradeYear() != null) {
          bg.setCurrentGradeYear(academicBg.getCurrentGradeYear());
        }

        if (academicBg.getSecondaryCompleted() != null) {
          bg.setSecondaryCompleted(academicBg.getSecondaryCompleted());
        }

        if (academicBg.getSecondaryDegreeTitle() != null) {
          bg.setSecondaryDegreeTitle(academicBg.getSecondaryDegreeTitle());
        }
      }

      // 3. Salud e Inclusión
      HealthInclusionDto health = data.getHealthInclusion();

      if (health != null) {
        ApplicantHealthInclusion inc = application.getHealthInclusion();

        if (inc == null) {
          inc = ApplicantHealthInclusion.builder().enrollmentApplication(application).build();
          application.setHealthInclusion(inc);
        }

        if (health.getReceivesReasonableAdjustments() != null) {
          inc.setReceivesReasonableAdjustments(health.getReceivesReasonableAdjustments());
        }

        if (health.getAdjustmentDetails() != null) {
          inc.setAdjustmentDetails(health.getAdjustmentDetails());
        }
      }

      // 4. Tutor / Responsable legal
      ResponsibleDto resp = data.getResponsible();

      if (resp != null) {
        ApplicantResponsible responsible = application.getResponsible();

        if (responsible == null) {
          responsible = ApplicantResponsible.builder().enrollmentApplication(application).build();
          application.setResponsible(responsible);
        }

        if (resp.getFullName() != null) {
          responsible.setFullName(resp.getFullName());
        }

        if (resp.getDocumentNumber() != null) {
          responsible.setDocumentNumber(resp.getDocumentNumber());
        }

        if (resp.getOccupation() != null) {
          responsible.setOccupation(resp.getOccupation());
        }

        if (resp.getPhoneNumber() != null) {
          responsible.setPhoneNumber(resp.getPhoneNumber());
        }

        if (resp.getEmail() != null) {
          responsible.setEmail(resp.getEmail());
        }

        if (resp.getEducationLevel() != null) {
          responsible.setEducationLevel(resp.getEducationLevel());
        }
      }

      // 5. Preferencias
      PreferenceDto pref = data.getPreference();

      if (pref != null) {
        ApplicantPreference preference = application.getPreference();

        if (preference == null) {
          preference = ApplicantPreference.builder().enrollmentApplication(application).build();
          application.setPreference(preference);
        }

        if (pref.getPreferredShift() != null) {
          preference.setPreferredShift(pref.getPreferredShift());
        }

        if (pref.getAllowsImageUse() != null) {
          preference.setAllowsImageUse(pref.getAllowsImageUse());
        }

        if (pref.getIsReenrolling() != null) {
          preference.setIsReenrolling(pref.getIsReenrolling());
        }

        if (pref.getPreviousTeacher() != null) {
          preference.setPreviousTeacher(pref.getPreviousTeacher());
        }
      }

      if (application.getStudyPlan() != null) {
        // 6. Validación de carrera/espacios/instrumentos. Si el trainingPath elegido
        // resuelve a un plan de estudio distinto del actual, ese pasa a ser el plan
        // efectivo de la solicitud y los espacios ya elegidos (que pertenecen al plan
        // viejo) se descartan.
        StudyPlan effectiveStudyPlan =
            enrollmentDraftDataValidator.validate(
                application.getInstitution().getId(), application, data);

        if (!effectiveStudyPlan.getId().equals(application.getStudyPlan().getId())) {
          // Changing plans only conflicts when a selected course is already requested
          // or enrolled through another application; the per-course availability
          // check below enforces that instead of blocking by training path.
          for (final var selection : application.getCourseSelections()) {
            validateCourseSelectionAvailability(application, selection.getCourse().getId());
          }

          application.changeStudyPlan(effectiveStudyPlan);
        }

        if (data.getAcademicSpaceSelection() != null
            && data.getAcademicSpaceSelection().getStudyPlanSpaceIds() != null) {
          Map<UUID, UUID> instrumentsMap =
              data.getInstrumentSelection() != null
                      && data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds() != null
                  ? data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds()
                  : Map.of();

          Set<UUID> desiredSpaceIds =
              new HashSet<>(data.getAcademicSpaceSelection().getStudyPlanSpaceIds());

          // Reconcile instead of clear+recreate: Hibernate flushes inserts before
          // orphan-removal deletes within the same transaction, so clearing and
          // re-adding an unchanged space here would violate the
          // (enrollment_application_id, study_plan_space_id) unique constraint on
          // every autosave, since the "old" row hasn't been deleted yet when the
          // "new" identical row is inserted.
          application
              .getSelectedSpaces()
              .removeIf(s -> !desiredSpaceIds.contains(s.getStudyPlanSpace().getId()));

          Map<UUID, EnrollmentApplicationSpace> existingByStudyPlanSpaceId =
              application.getSelectedSpaces().stream()
                  .collect(
                      Collectors.toMap(s -> s.getStudyPlanSpace().getId(), Function.identity()));

          for (UUID spaceId : desiredSpaceIds) {
            Instrument instrument = null;
            UUID instrumentId = instrumentsMap.get(spaceId);

            if (instrumentId != null) {
              instrument =
                  instrumentRepository
                      .findById(instrumentId)
                      .orElseThrow(
                          () ->
                              new EnrollmentValidationException(
                                  EnrollmentMessages.INSTRUMENT_ID_NOT_FOUND + instrumentId));
            }

            EnrollmentApplicationSpace existing = existingByStudyPlanSpaceId.get(spaceId);

            if (existing != null) {
              existing.setInstrument(instrument);
              continue;
            }

            StudyPlanSpace space =
                studyPlanSpaceRepository
                    .findById(spaceId)
                    .orElseThrow(
                        () ->
                            new EnrollmentValidationException(
                                EnrollmentMessages.SPACE_ID_NOT_FOUND + spaceId));

            EnrollmentApplicationSpace selectedSpace =
                EnrollmentApplicationSpace.builder()
                    .enrollmentApplication(application)
                    .studyPlanSpace(space)
                    .instrument(instrument)
                    .build();
            application.addSelectedSpace(selectedSpace);
          }
        }
      }

      // Existing drafts can retain a legacy study plan and still select concrete courses.
      if (data.getCourses() != null && courseSelectionsChanged(application, data.getCourses())) {
        updateCourseSelections(application, data.getCourses());
      }
    }

    EnrollmentApplication saved = saveAndFlush(application);

    return EnrollmentApplicationResponse.from(saved);
  }

  private void updateCourseSelections(
      final EnrollmentApplication application, final List<CourseSelectionDto> requestedSelections) {
    final Set<UUID> requestedCourseIds =
        requestedSelections.stream().map(CourseSelectionDto::courseId).collect(Collectors.toSet());
    if (requestedCourseIds.size() != requestedSelections.size()) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_SPACES_DUPLICATED);
    }

    final Map<UUID, EnrollmentApplicationCourse> existingByCourseId =
        application.getCourseSelections().stream()
            .collect(
                Collectors.toMap(selection -> selection.getCourse().getId(), Function.identity()));
    application
        .getCourseSelections()
        .removeIf(selection -> !requestedCourseIds.contains(selection.getCourse().getId()));

    for (final CourseSelectionDto requestedSelection : requestedSelections) {
      final var course =
          courseRepository
              .findByIdAndInstitution_Id(
                  requestedSelection.courseId(), application.getInstitution().getId())
              .orElseThrow(
                  () ->
                      new EnrollmentValidationException(
                          EnrollmentMessages.SPACE_ID_NOT_FOUND + requestedSelection.courseId()));
      if (!course.getAcademicYear().getId().equals(application.getAcademicYear().getId())
          || !course.isActive()
          || course.getStudyPlanSpace() == null
          || !course
              .getStudyPlanSpace()
              .getStudyPlan()
              .getTrainingPath()
              .getId()
              .equals(application.getTrainingPath().getId())) {
        throw new EnrollmentValidationException(
            EnrollmentMessages.ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID);
      }
      validateCourseSelectionAvailability(application, course.getId());

      final var preferredTeacher =
          requestedSelection.preferredTeacherId() == null
              ? null
              : personRepository
                  .findByIdAndInstitution_Id(
                      requestedSelection.preferredTeacherId(), application.getInstitution().getId())
                  .orElseThrow(
                      () ->
                          new EnrollmentValidationException(
                              EnrollmentMessages.PERSON_ID_NOT_FOUND
                                  + requestedSelection.preferredTeacherId()));
      if (preferredTeacher != null
          && !courseClassTeacherRepository.existsByCourseClass_Course_IdAndPerson_Id(
              course.getId(), preferredTeacher.getId())) {
        throw new EnrollmentValidationException(
            EnrollmentMessages.ENROLLMENT_APPLICATION_TEACHER_INVALID);
      }

      final var existing = existingByCourseId.get(course.getId());
      if (existing != null) {
        existing.changePreferredTeacher(preferredTeacher);
      } else {
        application.addCourseSelection(
            EnrollmentApplicationCourse.create(
                application.getInstitution(), application, course, preferredTeacher));
      }
    }
  }

  private boolean courseSelectionsChanged(
      final EnrollmentApplication application, final List<CourseSelectionDto> requestedSelections) {
    if (application.getCourseSelections().size() != requestedSelections.size()) {
      return true;
    }

    final Map<UUID, UUID> existingPreferredTeachers = new HashMap<>();
    for (final var selection : application.getCourseSelections()) {
      existingPreferredTeachers.put(
          selection.getCourse().getId(),
          selection.getPreferredTeacher() == null ? null : selection.getPreferredTeacher().getId());
    }
    for (final CourseSelectionDto requestedSelection : requestedSelections) {
      if (!existingPreferredTeachers.containsKey(requestedSelection.courseId())
          || !Objects.equals(
              existingPreferredTeachers.get(requestedSelection.courseId()),
              requestedSelection.preferredTeacherId())) {
        return true;
      }
    }

    return false;
  }

  private void validateCourseSelectionAvailability(
      final EnrollmentApplication application, final UUID courseId) {
    if (applicationCourseRepository.existsBlockingCourseSelectionExcludingApplication(
        application.getInstitution().getId(),
        courseId,
        application.getId(),
        application.getApplicantPerson().getId(),
        List.of(
            EnrollmentApplicationCourseStatus.PENDING,
            EnrollmentApplicationCourseStatus.WAITLISTED))) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ALREADY_REQUESTED);
    }

    final var student =
        studentRepository.findByInstitution_IdAndPerson_Id(
            application.getInstitution().getId(), application.getApplicantPerson().getId());
    if (student.isPresent()
        && courseEnrollmentRepository
            .findByStudentAndCourseAndStatus(
                application.getInstitution().getId(),
                student.get().getId(),
                courseId,
                CourseEnrollmentStatus.ENROLLED)
            .isPresent()) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ALREADY_ENROLLED);
    }
  }

  private EnrollmentApplication saveAndFlush(final EnrollmentApplication application) {
    try {
      return applicationRepository.saveAndFlush(application);
    } catch (DataIntegrityViolationException exception) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof ConstraintViolationException violation
            && "enrollment_application_courses_active_applicant_course_unique"
                .equals(violation.getConstraintName())) {
          throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ALREADY_REQUESTED);
        }
        if (cause instanceof ConstraintViolationException violation
            && "enrollment_application_courses_context_check"
                .equals(violation.getConstraintName())) {
          throw new EnrollmentValidationException(EnrollmentMessages.COURSE_NOT_ACTIVE);
        }
        if (cause instanceof ConstraintViolationException violation
            && ("enrollment_apps_applicant_active_path_unique".equals(violation.getConstraintName())
                || "enrollment_apps_applicant_active_draft_unique"
                    .equals(violation.getConstraintName())
                || "enrollment_apps_applicant_open_path_year_unique"
                    .equals(violation.getConstraintName()))) {
          throw new ActiveEnrollmentApplicationExistsException();
        }
      }

      throw exception;
    }
  }

  @Transactional
  public EnrollmentApplicationResponse cancelApplication(UUID personId, UUID applicationId) {
    final var institutionId =
        applicationRepository
            .findOwnedInstitutionId(applicationId, personId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));
    enrollmentInstitutionLock.lock(institutionId);
    EnrollmentApplication application =
        applicationRepository
            .findOwnedForUpdate(applicationId, personId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    application.cancel();
    for (final var selection : application.getCourseSelections()) {
      if (selection.isPendingResolution()) {
        selection.cancel(clock.instant(), personId);
      }
    }
    EnrollmentApplication saved = applicationRepository.save(application);

    return EnrollmentApplicationResponse.from(saved);
  }

  @Transactional
  public EnrollmentApplicationResponse submitApplication(UUID personId, UUID applicationId) {
    final var institutionId =
        applicationRepository
            .findOwnedInstitutionId(applicationId, personId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));
    enrollmentInstitutionLock.lock(institutionId);
    EnrollmentApplication application =
        applicationRepository
            .findOwnedForUpdate(applicationId, personId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    Map<String, String> errors = new HashMap<>();

    // 1. Validar datos personales
    Person applicant = application.getApplicantPerson();

    if (applicant == null) {
      errors.put("applicant", EnrollmentMessages.APPLICANT_REQUIRED);
    } else {
      if (applicant.getFirstName() == null || applicant.getFirstName().isBlank()) {
        errors.put("personalData.firstName", EnrollmentMessages.NAME_REQUIRED);
      }

      if (applicant.getLastName() == null || applicant.getLastName().isBlank()) {
        errors.put("personalData.lastName", EnrollmentMessages.LAST_NAME_REQUIRED);
      }

      if (applicant.getDocumentNumber() == null || applicant.getDocumentNumber().isBlank()) {
        errors.put("personalData.documentNumber", EnrollmentMessages.DOCUMENT_REQUIRED);
      }

      if (applicant.getEmail() == null || applicant.getEmail().isBlank()) {
        errors.put("personalData.email", EnrollmentMessages.EMAIL_REQUIRED);
      }

      if (applicant.getBirthDate() != null) {
        int age = Period.between(applicant.getBirthDate(), businessDateProvider.today()).getYears();

        if (age < 18) {
          ApplicantResponsible resp = application.getResponsible();

          if (resp == null) {
            errors.put("responsible", EnrollmentMessages.RESPONSIBLE_REQUIRED);
          } else {
            if (resp.getFullName() == null || resp.getFullName().isBlank()) {
              errors.put("responsible.fullName", EnrollmentMessages.RESPONSIBLE_NAME_REQUIRED);
            }

            if (resp.getDocumentNumber() == null || resp.getDocumentNumber().isBlank()) {
              errors.put(
                  "responsible.documentNumber", EnrollmentMessages.RESPONSIBLE_DOCUMENT_REQUIRED);
            }

            if (resp.getPhoneNumber() == null || resp.getPhoneNumber().isBlank()) {
              errors.put("responsible.phoneNumber", EnrollmentMessages.RESPONSIBLE_PHONE_REQUIRED);
            }
          }
        }
      }
    }

    // 2. Validar antecedentes académicos
    ApplicantEducationBackground edu = application.getEducationBackground();

    if (edu == null
        || ((edu.getSecondarySchool() == null || edu.getSecondarySchool().isBlank())
            && (edu.getSchoolOrigin() == null || edu.getSchoolOrigin().isBlank()))) {
      errors.put("academicBackground", EnrollmentMessages.EDUCATION_REQUIRED);
    }

    // 3. Validar la selección de cursos concretos.
    final boolean hasCourseSelections =
        application.getCourseSelections() != null && !application.getCourseSelections().isEmpty();
    if (!hasCourseSelections) {
      errors.put("courses", EnrollmentMessages.ENROLLMENT_APPLICATION_SPACES_REQUIRED);
    }

    // 4. Validar preferencias
    ApplicantPreference pref = application.getPreference();

    if (pref == null || pref.getPreferredShift() == null || pref.getPreferredShift().isBlank()) {
      errors.put("preference.preferredShift", EnrollmentMessages.SHIFT_REQUIRED);
    } else if (pref.isReenrolling()
        && (pref.getPreviousTeacher() == null || pref.getPreviousTeacher().isBlank())) {
      errors.put("preference.previousTeacher", EnrollmentMessages.PREVIOUS_TEACHER_REQUIRED);
    }

    if (!errors.isEmpty()) {
      throw new EnrollmentValidationException(EnrollmentMessages.SUBMISSION_INCOMPLETE, errors);
    }

    if (application.getCourseSelections() != null && !application.getCourseSelections().isEmpty()) {
      periodRepository
          .findActivePeriod(
              application.getInstitution().getId(),
              application.getAcademicYear().getId(),
              EnrollmentPeriodStatus.OPEN,
              clock.instant())
          .orElseThrow(EnrollmentPeriodClosedException::new);
      updateCourseSelections(
          application,
          application.getCourseSelections().stream()
              .map(
                  selection ->
                      new CourseSelectionDto(
                          selection.getCourse().getId(),
                          selection.getPreferredTeacher() == null
                              ? null
                              : selection.getPreferredTeacher().getId()))
              .toList());
      applicationCourseApprovalService.markSubmitted(application);
    }
    application.submit();
    EnrollmentApplication saved = applicationRepository.save(application);

    return EnrollmentApplicationResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<EnrollmentApplicationResponse> listApplications(
      UUID institutionId,
      @Nullable UUID periodId,
      @Nullable EnrollmentApplicationStatus status,
      @Nullable String search,
      Pageable pageable) {

    Page<EnrollmentApplication> page =
        applicationRepository.findAll(
            byListFilters(institutionId, periodId, status, search), pageable);

    return PaginatedResponse.from(page.map(EnrollmentApplicationResponse::from));
  }

  @Transactional(readOnly = true)
  public EnrollmentApplicationResponse getApplicationById(UUID personId, UUID applicationId) {
    return getApplicationById(null, personId, applicationId);
  }

  @Transactional(readOnly = true)
  public EnrollmentApplicationResponse getApplicationById(
      @Nullable UUID institutionId, @Nullable UUID personId, UUID applicationId) {
    EnrollmentApplication application =
        applicationRepository
            .findById(applicationId)
            .filter(app -> app.getDeletedAt() == null)
            .filter(
                app ->
                    (personId != null && app.getApplicantPerson().getId().equals(personId))
                        || (institutionId != null
                            && app.getInstitution().getId().equals(institutionId)))
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    return EnrollmentApplicationResponse.from(application);
  }

  private Specification<EnrollmentApplication> byListFilters(
      UUID institutionId,
      @Nullable UUID periodId,
      @Nullable EnrollmentApplicationStatus status,
      @Nullable String search) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("institution").get("id"), institutionId));
      predicates.add(cb.isNull(root.get("deletedAt")));

      if (periodId != null) {
        predicates.add(cb.equal(root.get("enrollmentPeriod").get("id"), periodId));
      }

      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }

      String normalizedSearch = SearchNormalization.normalizeSearch(search);

      if (normalizedSearch != null) {
        String pattern = SearchNormalization.likeContainsPattern(normalizedSearch);
        var personJoin = root.join("applicantPerson");
        predicates.add(
            cb.or(
                cb.like(
                    SearchNormalization.unaccentLower(cb, personJoin.get("firstName")),
                    pattern,
                    '\\'),
                cb.like(
                    SearchNormalization.unaccentLower(cb, personJoin.get("lastName")),
                    pattern,
                    '\\'),
                cb.like(
                    SearchNormalization.unaccentLower(cb, personJoin.get("documentNumber")),
                    pattern,
                    '\\')));
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
