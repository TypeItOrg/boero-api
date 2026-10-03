package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.audit.enums.AuditAction;
import ar.edu.utn.frvm.typeit.boero_api.audit.enums.AuditEntityType;
import ar.edu.utn.frvm.typeit.boero_api.audit.services.AuditEventRecorder;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import ar.edu.utn.frvm.typeit.boero_api.common.time.BusinessDateProvider;
import ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantEducationBackground;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantHealthInclusion;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantPreference;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantResponsible;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationSpace;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ActiveEnrollmentApplicationExistsException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AcademicBackgroundDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.HealthInclusionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PreferenceDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.ResponsibleDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.GuardianLinkStatus;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.UnauthorizedGuardianshipException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonGuardianRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final PersonGuardianRepository personGuardianRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final AcademicYearRepository academicYearRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final InstrumentRepository instrumentRepository;
  private final EnrollmentDraftDataValidator enrollmentDraftDataValidator;
  private final BusinessDateProvider businessDateProvider;
  private final Clock clock;
  private final AuditEventRecorder auditEventRecorder;

  @Transactional
  public EnrollmentApplicationResponse startOrGetApplication(
      UUID institutionId, UUID personId, StartEnrollmentApplicationRequest request) {
    // Without applicantPersonId the caller applies for themself. Otherwise they act as a tutor of
    // that person, which must be backed by an existing guardianship link. No application exists
    // yet here, so the link is checked per person pair; afterwards the same person_guardians rows
    // back EnrollmentApplicationRepository.ACCESSIBLE_BY_PERSON.
    final UUID applicantPersonId =
        request.getApplicantPersonId() != null ? request.getApplicantPersonId() : personId;
    final boolean actingForDependent = !applicantPersonId.equals(personId);

    if (!actingForDependent
        && personRoleAssignmentRepository.existsByPerson_IdAndInstitution_IdAndRole_Code(
            personId, institutionId, SystemRoleCode.GUARDIAN.name())) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_GUARDIAN_MUST_USE_DEPENDENT);
    }

    if (actingForDependent
        && !personGuardianRepository
            .existsByInstitution_IdAndTutorPerson_IdAndDependentPerson_IdAndStatusIn(
                institutionId, personId, applicantPersonId, List.of(GuardianLinkStatus.ACTIVE))) {
      throw new UnauthorizedGuardianshipException();
    }

    Person person =
        personRepository
            .findByIdAndInstitution_Id(applicantPersonId, institutionId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.ENROLLMENT_APPLICATION_APPLICANT_REQUIRED));
    // 1. Buscar borrador existente activo
    Optional<EnrollmentApplication> existingDraft =
        applicationRepository
            .findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                applicantPersonId,
                request.getStudyPlanId(),
                request.getAcademicYearId(),
                EnrollmentApplicationStatus.DRAFT)
            .filter(app -> app.getInstitution().getId().equals(institutionId))
            .filter(app -> app.getStudyPlan().getInstitution().getId().equals(institutionId));

    if (existingDraft.isPresent()) {
      return EnrollmentApplicationResponse.from(existingDraft.get());
    }

    StudyPlan requestedStudyPlan =
        studyPlanRepository
            .findAvailableOfferById(
                institutionId, request.getStudyPlanId(), businessDateProvider.today())
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID));

    // 2. Validar período de inscripción abierto
    EnrollmentPeriod period =
        periodRepository
            .findActivePeriod(
                institutionId,
                request.getAcademicYearId(),
                EnrollmentPeriodStatus.OPEN,
                clock.instant())
            .orElseThrow(EnrollmentPeriodClosedException::new);

    // 3. Una única inscripción viva por trayecto: si ya tiene una solicitud
    // no cancelada/rechazada en el trayecto del plan pedido (aunque sea para
    // otro plan de estudio o ciclo lectivo), no se permite iniciar otra.
    boolean hasActiveApplicationInTrainingPath =
        !applicationRepository
            .findActiveByApplicantPersonIdAndTrainingPathId(
                applicantPersonId, requestedStudyPlan.getTrainingPath().getId())
            .isEmpty();

    if (hasActiveApplicationInTrainingPath) {
      throw new ActiveEnrollmentApplicationExistsException();
    }

    AcademicYear academicYear =
        academicYearRepository
            .findByIdAndInstitution_Id(request.getAcademicYearId(), institutionId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.ACADEMIC_YEAR_ID_NOT_FOUND
                            + request.getAcademicYearId()));

    final Person submitter =
        actingForDependent
            ? personRepository
                .findByIdAndInstitution_Id(personId, institutionId)
                .orElseThrow(
                    () ->
                        new EnrollmentValidationException(
                            EnrollmentMessages.ENROLLMENT_APPLICATION_APPLICANT_REQUIRED))
            : person;

    EnrollmentApplication newApplication =
        EnrollmentApplication.builder()
            .institution(period.getInstitution())
            .applicantPerson(person)
            .submittedByPerson(submitter)
            .studyPlan(requestedStudyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();

    if (actingForDependent) {
      // The tutor is the natural legal responsible; saves them retyping their own data.
      newApplication.setResponsible(responsibleFrom(submitter));
    }

    EnrollmentApplication saved = saveAndFlush(newApplication);
    audit(personId, saved, AuditAction.ENROLLMENT_APPLICATION_STARTED);

    return EnrollmentApplicationResponse.from(saved);
  }

  private ApplicantResponsible responsibleFrom(final Person tutor) {
    return ApplicantResponsible.builder()
        .fullName(tutor.getFirstName() + " " + tutor.getLastName())
        .documentNumber(tutor.getDocumentNumber())
        .phoneNumber(tutor.getPhoneNumber())
        .email(tutor.getEmail())
        .build();
  }

  @Transactional
  public EnrollmentApplicationResponse updateDraft(
      UUID personId, UUID applicationId, UpdateEnrollmentDraftRequest request) {
    EnrollmentApplication application =
        applicationRepository
            .findAccessibleForUpdate(applicationId, personId)
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

      // 6. Validación de carrera/espacios/instrumentos. Si el trainingPath elegido
      // resuelve a un plan de estudio distinto del actual, ese pasa a ser el plan
      // efectivo de la solicitud y los espacios ya elegidos (que pertenecen al plan
      // viejo) se descartan.
      StudyPlan effectiveStudyPlan =
          enrollmentDraftDataValidator.validate(
              application.getInstitution().getId(), application, data);

      if (!effectiveStudyPlan.getId().equals(application.getStudyPlan().getId())) {
        boolean hasOtherActiveApplication =
            applicationRepository
                .findActiveByApplicantPersonIdAndTrainingPathId(
                    application.getApplicantPerson().getId(),
                    effectiveStudyPlan.getTrainingPath().getId())
                .stream()
                .anyMatch(other -> !other.getId().equals(applicationId));

        if (hasOtherActiveApplication) {
          throw new ActiveEnrollmentApplicationExistsException();
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
                .collect(Collectors.toMap(s -> s.getStudyPlanSpace().getId(), Function.identity()));

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

    EnrollmentApplication saved = saveAndFlush(application);
    audit(personId, saved, AuditAction.ENROLLMENT_APPLICATION_DRAFT_UPDATED);

    return EnrollmentApplicationResponse.from(saved);
  }

  /** The subject is always the applicant; the actor may be the applicant or their tutor. */
  private void audit(
      final UUID actorPersonId, final EnrollmentApplication application, final AuditAction action) {
    auditEventRecorder.record(
        application.getInstitution(),
        actorPersonId,
        application.getApplicantPerson().getId(),
        action,
        AuditEntityType.ENROLLMENT_APPLICATION,
        application.getId());
  }

  private EnrollmentApplication saveAndFlush(final EnrollmentApplication application) {
    try {
      return applicationRepository.saveAndFlush(application);
    } catch (DataIntegrityViolationException exception) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof ConstraintViolationException violation
            && ("enrollment_apps_applicant_active_path_unique".equals(violation.getConstraintName())
                || "enrollment_apps_applicant_active_draft_unique"
                    .equals(violation.getConstraintName()))) {
          throw new ActiveEnrollmentApplicationExistsException();
        }
      }

      throw exception;
    }
  }

  @Transactional
  public EnrollmentApplicationResponse cancelApplication(UUID personId, UUID applicationId) {
    EnrollmentApplication application =
        applicationRepository
            .findAccessibleForUpdate(applicationId, personId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    application.cancel();
    EnrollmentApplication saved = applicationRepository.save(application);
    audit(personId, saved, AuditAction.ENROLLMENT_APPLICATION_CANCELLED);

    return EnrollmentApplicationResponse.from(saved);
  }

  @Transactional
  public EnrollmentApplicationResponse submitApplication(UUID personId, UUID applicationId) {
    EnrollmentApplication application =
        applicationRepository
            .findAccessibleForUpdate(applicationId, personId)
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

      // Who is logged in decides the rule: someone applying for themself must be an adult, while a
      // tutor (access is already guaranteed by the lookup above) can only act for a minor.
      final boolean selfService = personId.equals(applicant.getId());

      if (applicant.getBirthDate() == null) {
        errors.put("personalData.birthDate", EnrollmentMessages.BIRTH_DATE_REQUIRED);
      } else {
        final boolean minor =
            Period.between(applicant.getBirthDate(), businessDateProvider.today()).getYears()
                < PersonFieldConstraints.ADULT_AGE;

        if (selfService && minor) {
          errors.put("personalData.birthDate", EnrollmentMessages.APPLICANT_MUST_BE_ADULT);
        } else if (!selfService && !minor) {
          errors.put("personalData.birthDate", EnrollmentMessages.DEPENDENT_MUST_BE_MINOR);
        }
      }

      // A minor has no email of their own: the contact goes in the responsible's data.
      if (selfService && (applicant.getEmail() == null || applicant.getEmail().isBlank())) {
        errors.put("personalData.email", EnrollmentMessages.EMAIL_REQUIRED);
      }

      if (!selfService) {
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

    // 2. Validar antecedentes académicos
    ApplicantEducationBackground edu = application.getEducationBackground();

    if (edu == null
        || ((edu.getSecondarySchool() == null || edu.getSecondarySchool().isBlank())
            && (edu.getSchoolOrigin() == null || edu.getSchoolOrigin().isBlank()))) {
      errors.put("academicBackground", EnrollmentMessages.EDUCATION_REQUIRED);
    }

    // 3. Validar selección de espacios académicos
    if (application.getSelectedSpaces() == null || application.getSelectedSpaces().isEmpty()) {
      errors.put(
          "academicSpaceSelection.studyPlanSpaceIds",
          EnrollmentMessages.ENROLLMENT_APPLICATION_SPACES_REQUIRED);
    } else {
      enrollmentDraftDataValidator.validateSubmission(application);
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

    application.submit();
    EnrollmentApplication saved = applicationRepository.save(application);
    audit(personId, saved, AuditAction.ENROLLMENT_APPLICATION_SUBMITTED);

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
                    (personId != null
                            && (app.getApplicantPerson().getId().equals(personId)
                                || applicationRepository.isAccessibleByPerson(
                                    app.getId(), personId)))
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
