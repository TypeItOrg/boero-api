package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
  private final ObjectMapper objectMapper;

  @Transactional
  public EnrollmentApplicationResponse startOrGetApplication(
      UUID institutionId, UUID personId, StartEnrollmentApplicationRequest request) {

    // 1. Buscar borrador existente activo
    Optional<EnrollmentApplication> existingDraft =
        applicationRepository.findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
            personId,
            request.getStudyPlanId(),
            request.getAcademicYearId(),
            EnrollmentApplicationStatus.DRAFT);

    if (existingDraft.isPresent()) {
      return toResponse(existingDraft.get());
    }

    // 2. Si no existe, validar período de inscripción activo
    LocalDateTime now = LocalDateTime.now();
    EnrollmentPeriod activePeriod =
        periodRepository
            .findActivePeriod(
                institutionId, request.getAcademicYearId(), EnrollmentPeriodStatus.OPEN, now)
            .orElseThrow(EnrollmentPeriodClosedException::new);

    // 3. Obtener referencias de dominio
    Person applicant =
        personRepository
            .findById(personId)
            .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
    StudyPlan studyPlan =
        studyPlanRepository
            .findById(request.getStudyPlanId())
            .orElseThrow(() -> new IllegalArgumentException("Plan de estudio no encontrado"));
    AcademicYear academicYear =
        academicYearRepository
            .findById(request.getAcademicYearId())
            .orElseThrow(() -> new IllegalArgumentException("Ciclo lectivo no encontrado"));

    // 4. Crear nuevo borrador
    EnrollmentApplication newApplication =
        EnrollmentApplication.builder()
            .institution(activePeriod.getInstitution())
            .applicantPerson(applicant)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(activePeriod)
            .status(EnrollmentApplicationStatus.DRAFT)
            .data("{}")
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

    String jsonString = "{}";
    if (request.getData() != null) {
      try {
        jsonString = objectMapper.writeValueAsString(request.getData());
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Formato de datos inválido", e);
      }
    }

    application.setData(jsonString);
    EnrollmentApplication saved = applicationRepository.save(application);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public EnrollmentApplicationResponse getApplicationById(UUID personId, UUID applicationId) {
    EnrollmentApplication application =
        applicationRepository
            .findById(applicationId)
            .filter(app -> app.getApplicantPerson().getId().equals(personId))
            .filter(app -> app.getDeletedAt() == null)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));

    return toResponse(application);
  }

  private EnrollmentApplicationResponse toResponse(EnrollmentApplication entity) {
    Map<String, Object> dataMap = new HashMap<>();
    if (entity.getData() != null && !entity.getData().isBlank()) {
      try {
        dataMap = objectMapper.readValue(entity.getData(), new TypeReference<>() {});
      } catch (JsonProcessingException e) {
        // Fallback si la cadena json es inválida
      }
    }

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
        .data(dataMap)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
