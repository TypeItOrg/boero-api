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
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentAttachmentType;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AcademicBackgroundDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AcademicSpaceSelectionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AttachmentDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CareerSelectionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.HealthInclusionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.InstrumentSelectionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PersonalDataDto;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
      return toResponse(existingDraft.get());
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

    StudyPlan studyPlan =
        studyPlanRepository
            .findById(request.getStudyPlanId())
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        "No se encontró el plan de estudio con ID " + request.getStudyPlanId()));

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
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();

    EnrollmentApplication saved = applicationRepository.save(newApplication);
    return toResponse(saved);
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

      // 6. Validación y Persistencia de Espacios Curriculares e Instrumentos
      enrollmentDraftDataValidator.validate(application.getInstitution().getId(), application, data);

      if (data.getAcademicSpaceSelection() != null
          && data.getAcademicSpaceSelection().getStudyPlanSpaceIds() != null) {
        application.clearSelectedSpaces();
        Map<UUID, UUID> instrumentsMap =
            data.getInstrumentSelection() != null
                    && data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds() != null
                ? data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds()
                : Map.of();

        for (UUID spaceId : data.getAcademicSpaceSelection().getStudyPlanSpaceIds()) {
          StudyPlanSpace space =
              studyPlanSpaceRepository
                  .findById(spaceId)
                  .orElseThrow(
                      () ->
                          new EnrollmentValidationException(
                              "Espacio de plan de estudio no encontrado: " + spaceId));

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
    return toResponse(saved);
  }

  @Transactional
  public EnrollmentApplicationResponse cancelApplication(UUID personId, UUID applicationId) {
    EnrollmentApplication application =
        applicationRepository
            .findById(applicationId)
            .filter(app -> app.getApplicantPerson().getId().equals(personId))
            .filter(app -> app.getDeletedAt() == null)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    application.setStatus(EnrollmentApplicationStatus.CANCELLED);
    EnrollmentApplication saved = applicationRepository.save(application);
    return toResponse(saved);
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

    // 3. Validar salud e inclusión
    ApplicantHealthInclusion health = application.getHealthInclusion();
    if (health != null && health.isReceivesReasonableAdjustments()) {
      boolean hasHealthDoc =
          application.getAttachments() != null
              && application.getAttachments().stream()
                  .anyMatch(
                      att ->
                          att.getDeletedAt() == null
                              && att.getAttachmentType() == EnrollmentAttachmentType.HEALTH_REPORT);
      if (!hasHealthDoc) {
        errors.put(
            "healthInclusion.report",
            "Es obligatorio adjuntar el informe profesional si se requieren ajustes razonables");
      }
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

    application.setStatus(EnrollmentApplicationStatus.SUBMITTED);
    EnrollmentApplication saved = applicationRepository.save(application);
    return toResponse(saved);
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

    return PaginatedResponse.from(page.map(this::toResponse));
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

    return toResponse(application);
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

  private EnrollmentApplicationResponse toResponse(EnrollmentApplication entity) {
    PersonalDataDto personalDataDto = null;
    Person applicant = entity.getApplicantPerson();
    if (applicant != null) {
      personalDataDto =
          PersonalDataDto.builder()
              .firstName(applicant.getFirstName())
              .lastName(applicant.getLastName())
              .documentNumber(applicant.getDocumentNumber())
              .birthDate(applicant.getBirthDate())
              .phoneNumber(applicant.getPhoneNumber())
              .email(applicant.getEmail())
              .build();
    }

    AcademicBackgroundDto academicBgDto = null;
    ApplicantEducationBackground bg = entity.getEducationBackground();
    if (bg != null) {
      academicBgDto =
          AcademicBackgroundDto.builder()
              .secondarySchool(bg.getSecondarySchool())
              .schoolOrigin(bg.getSchoolOrigin())
              .currentGradeYear(bg.getCurrentGradeYear())
              .secondaryCompleted(bg.isSecondaryCompleted())
              .secondaryDegreeTitle(bg.getSecondaryDegreeTitle())
              .build();
    }

    HealthInclusionDto healthDto = null;
    ApplicantHealthInclusion health = entity.getHealthInclusion();
    if (health != null) {
      healthDto =
          HealthInclusionDto.builder()
              .receivesReasonableAdjustments(health.isReceivesReasonableAdjustments())
              .adjustmentDetails(health.getAdjustmentDetails())
              .build();
    }

    ResponsibleDto responsibleDto = null;
    ApplicantResponsible resp = entity.getResponsible();
    if (resp != null) {
      responsibleDto =
          ResponsibleDto.builder()
              .fullName(resp.getFullName())
              .documentNumber(resp.getDocumentNumber())
              .occupation(resp.getOccupation())
              .phoneNumber(resp.getPhoneNumber())
              .email(resp.getEmail())
              .educationLevel(resp.getEducationLevel())
              .build();
    }

    PreferenceDto preferenceDto = null;
    ApplicantPreference pref = entity.getPreference();
    if (pref != null) {
      preferenceDto =
          PreferenceDto.builder()
              .preferredShift(pref.getPreferredShift())
              .allowsImageUse(pref.isAllowsImageUse())
              .isReenrolling(pref.isReenrolling())
              .previousTeacher(pref.getPreviousTeacher())
              .build();
    }

    AcademicSpaceSelectionDto spaceSelectionDto = null;
    InstrumentSelectionDto instrumentSelectionDto = null;
    if (entity.getSelectedSpaces() != null && !entity.getSelectedSpaces().isEmpty()) {
      List<UUID> spaceIds =
          entity.getSelectedSpaces().stream()
              .filter(s -> s.getDeletedAt() == null)
              .map(s -> s.getStudyPlanSpace().getId())
              .toList();
      spaceSelectionDto = new AcademicSpaceSelectionDto(spaceIds);

      Map<UUID, UUID> instMap =
          entity.getSelectedSpaces().stream()
              .filter(s -> s.getDeletedAt() == null && s.getInstrument() != null)
              .collect(
                  Collectors.toMap(
                      s -> s.getStudyPlanSpace().getId(), s -> s.getInstrument().getId()));
      instrumentSelectionDto = new InstrumentSelectionDto(instMap);
    }

    CareerSelectionDto careerDto = null;
    if (entity.getStudyPlan() != null && entity.getStudyPlan().getTrainingPath() != null) {
      careerDto = new CareerSelectionDto(entity.getStudyPlan().getTrainingPath().getId());
    }

    List<AttachmentDto> attachmentsList = new ArrayList<>();
    if (entity.getAttachments() != null) {
      attachmentsList =
          entity.getAttachments().stream()
              .filter(att -> att.getDeletedAt() == null)
              .map(
                  att ->
                      AttachmentDto.builder()
                          .id(att.getId())
                          .attachmentType(
                              att.getAttachmentType() != null
                                  ? att.getAttachmentType().name()
                                  : null)
                          .originalFileName(att.getOriginalFileName())
                          .storagePath(att.getStoragePath())
                          .contentType(att.getContentType())
                          .fileSize(att.getFileSize())
                          .createdAt(att.getCreatedAt())
                          .build())
              .toList();
    }

    EnrollmentDraftData draftData =
        EnrollmentDraftData.builder()
            .personalData(personalDataDto != null ? personalDataDto : new PersonalDataDto())
            .academicBackground(academicBgDto != null ? academicBgDto : new AcademicBackgroundDto())
            .healthInclusion(healthDto != null ? healthDto : new HealthInclusionDto())
            .responsible(responsibleDto != null ? responsibleDto : new ResponsibleDto())
            .preference(preferenceDto != null ? preferenceDto : new PreferenceDto())
            .careerSelection(careerDto != null ? careerDto : new CareerSelectionDto())
            .academicSpaceSelection(
                spaceSelectionDto != null ? spaceSelectionDto : new AcademicSpaceSelectionDto())
            .instrumentSelection(
                instrumentSelectionDto != null
                    ? instrumentSelectionDto
                    : new InstrumentSelectionDto())
            .attachments(attachmentsList)
            .build();

    boolean editable =
        entity.getStatus() == EnrollmentApplicationStatus.DRAFT && entity.getDeletedAt() == null;

    return EnrollmentApplicationResponse.builder()
        .applicationId(entity.getId())
        .institutionId(entity.getInstitution().getId())
        .personId(entity.getApplicantPerson().getId())
        .studyPlanId(entity.getStudyPlan().getId())
        .academicYearId(entity.getAcademicYear().getId())
        .enrollmentPeriodId(entity.getEnrollmentPeriod().getId())
        .status(entity.getStatus())
        .isEditable(editable)
        .data(draftData)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
