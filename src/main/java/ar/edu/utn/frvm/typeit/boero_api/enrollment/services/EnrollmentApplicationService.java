package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
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
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AcademicBackgroundDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.HealthInclusionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PreferenceDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.ResponsibleDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.jspecify.annotations.Nullable;
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
  private final AcademicYearRepository academicYearRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final InstrumentRepository instrumentRepository;
  private final EnrollmentDraftDataValidator enrollmentDraftDataValidator;

  @Transactional
  public EnrollmentApplicationResponse startOrGetApplication(
      UUID institutionId, UUID personId, StartEnrollmentApplicationRequest request) {

    // 1. Buscar borrador existente activo
    Optional<EnrollmentApplication> existingDraft =
        applicationRepository
            .findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                personId,
                request.getStudyPlanId(),
                request.getAcademicYearId(),
                EnrollmentApplicationStatus.DRAFT);

    if (existingDraft.isPresent()) {
      return EnrollmentApplicationResponse.from(existingDraft.get());
    }

    // 2. Validar período de inscripción abierto
    EnrollmentPeriod period =
        periodRepository
            .findActivePeriod(
                institutionId,
                request.getAcademicYearId(),
                EnrollmentPeriodStatus.OPEN,
                LocalDateTime.now())
            .orElseThrow(EnrollmentPeriodClosedException::new);

    Person person =
        personRepository
            .findById(personId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        "No se encontró la persona postulante con ID " + personId));

    StudyPlan requestedStudyPlan =
        studyPlanRepository
            .findById(request.getStudyPlanId())
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        "No se encontró el plan de estudio con ID " + request.getStudyPlanId()));

    // 3. Una única inscripción viva por trayecto: si ya tiene una solicitud
    // no cancelada/rechazada en el trayecto del plan pedido (aunque sea para
    // otro plan de estudio o ciclo lectivo), no se permite iniciar otra.
    boolean hasActiveApplicationInTrainingPath =
        !applicationRepository
            .findActiveByApplicantPersonIdAndTrainingPathId(
                personId, requestedStudyPlan.getTrainingPath().getId())
            .isEmpty();

    if (hasActiveApplicationInTrainingPath) {
      throw new ActiveEnrollmentApplicationExistsException();
    }

    AcademicYear academicYear =
        academicYearRepository
            .findById(request.getAcademicYearId())
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        "No se encontró el ciclo lectivo con ID " + request.getAcademicYearId()));

    EnrollmentApplication newApplication =
        EnrollmentApplication.builder()
            .institution(period.getInstitution())
            .applicantPerson(person)
            .studyPlan(requestedStudyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();

    EnrollmentApplication saved = applicationRepository.save(newApplication);
    return EnrollmentApplicationResponse.from(saved);
  }

  @Transactional
  public EnrollmentApplicationResponse updateDraft(
      UUID personId, UUID applicationId, UpdateEnrollmentDraftRequest request) {
    EnrollmentApplication application =
        applicationRepository
            .findById(applicationId)
            .filter(app -> app.getApplicantPerson().getId().equals(personId))
            .filter(app -> app.getDeletedAt() == null)
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
        application.setStudyPlan(effectiveStudyPlan);
        application.clearSelectedSpaces();
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
                                "Instrumento no encontrado: " + instrumentId));
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
                              "Espacio de plan de estudio no encontrado: " + spaceId));

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

    EnrollmentApplication saved = applicationRepository.save(application);
    return EnrollmentApplicationResponse.from(saved);
  }

  @Transactional
  public EnrollmentApplicationResponse cancelApplication(UUID personId, UUID applicationId) {
    EnrollmentApplication application =
        applicationRepository
            .findById(applicationId)
            .filter(app -> app.getApplicantPerson().getId().equals(personId))
            .filter(app -> app.getDeletedAt() == null)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    application.cancel();
    EnrollmentApplication saved = applicationRepository.save(application);
    return EnrollmentApplicationResponse.from(saved);
  }

  @Transactional
  public EnrollmentApplicationResponse submitApplication(UUID personId, UUID applicationId) {
    EnrollmentApplication application =
        applicationRepository
            .findById(applicationId)
            .filter(app -> app.getApplicantPerson().getId().equals(personId))
            .filter(app -> app.getDeletedAt() == null)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    Map<String, String> errors = new HashMap<>();

    // 1. Validar datos personales
    Person applicant = application.getApplicantPerson();
    if (applicant == null) {
      errors.put("applicant", "Los datos del aspirante son obligatorios");
    } else {
      if (applicant.getFirstName() == null || applicant.getFirstName().isBlank()) {
        errors.put("personalData.firstName", "El nombre es obligatorio");
      }
      if (applicant.getLastName() == null || applicant.getLastName().isBlank()) {
        errors.put("personalData.lastName", "El apellido es obligatorio");
      }
      if (applicant.getDocumentNumber() == null || applicant.getDocumentNumber().isBlank()) {
        errors.put("personalData.documentNumber", "El número de documento es obligatorio");
      }
      if (applicant.getEmail() == null || applicant.getEmail().isBlank()) {
        errors.put("personalData.email", "El correo electrónico es obligatorio");
      }

      if (applicant.getBirthDate() != null) {
        int age = Period.between(applicant.getBirthDate(), LocalDate.now()).getYears();
        if (age < 18) {
          ApplicantResponsible resp = application.getResponsible();
          if (resp == null) {
            errors.put(
                "responsible",
                "Los postulantes menores de 18 años deben incluir los datos del tutor o responsable legal");
          } else {
            if (resp.getFullName() == null || resp.getFullName().isBlank()) {
              errors.put("responsible.fullName", "El nombre del responsable es obligatorio");
            }
            if (resp.getDocumentNumber() == null || resp.getDocumentNumber().isBlank()) {
              errors.put(
                  "responsible.documentNumber", "El documento del responsable es obligatorio");
            }
            if (resp.getPhoneNumber() == null || resp.getPhoneNumber().isBlank()) {
              errors.put("responsible.phoneNumber", "El teléfono del responsable es obligatorio");
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
      errors.put("academicBackground", "Los antecedentes educativos son obligatorios");
    }

    // 3. Validar selección de espacios académicos
    if (application.getSelectedSpaces() == null || application.getSelectedSpaces().isEmpty()) {
      errors.put(
          "academicSpaceSelection.studyPlanSpaceIds",
          EnrollmentMessages.ENROLLMENT_APPLICATION_SPACES_REQUIRED);
    }

    // 4. Validar preferencias
    ApplicantPreference pref = application.getPreference();
    if (pref == null || pref.getPreferredShift() == null || pref.getPreferredShift().isBlank()) {
      errors.put("preference.preferredShift", "El turno preferido es obligatorio");
    } else if (pref.isReenrolling()
        && (pref.getPreviousTeacher() == null || pref.getPreviousTeacher().isBlank())) {
      errors.put(
          "preference.previousTeacher",
          "El docente previo es obligatorio para aspirantes reingresantes");
    }

    if (!errors.isEmpty()) {
      throw new EnrollmentValidationException(
          "Existen campos obligatorios sin completar para enviar la inscripción", errors);
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
