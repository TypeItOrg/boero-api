package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(
    requiredProperties = {
      "personalData",
      "academicBackground",
      "healthInclusion",
      "responsible",
      "preference",
      "careerSelection",
      "academicSpaceSelection",
      "instrumentSelection",
      "courses",
      "attachments"
    })
public record EnrollmentDraftData(
    @Schema(nullable = true) PersonalDataDto personalData,
    @Schema(
            nullable = true,
            description =
                "Si se incluye, la escolaridad adaptable se reemplaza como una fotografía completa: los campos nulos eliminan respuestas anteriores. Si se omite, se conserva sin cambios.")
        @Valid
        AcademicBackgroundDto academicBackground,
    @Schema(nullable = true) HealthInclusionDto healthInclusion,
    @Schema(nullable = true) ResponsibleDto responsible,
    @Schema(nullable = true) PreferenceDto preference,
    @Schema(nullable = true) CareerSelectionDto careerSelection,
    @Schema(nullable = true) AcademicSpaceSelectionDto academicSpaceSelection,
    @Schema(nullable = true) InstrumentSelectionDto instrumentSelection,
    @Schema(nullable = true) List<CourseSelectionDto> courses,
    @Schema(nullable = true) List<AttachmentDto> attachments) {
  public EnrollmentDraftData() {
    this(null, null, null, null, null, null, null, null, new ArrayList<>(), new ArrayList<>());
  }

  public EnrollmentDraftData(
      final PersonalDataDto personalData,
      final AcademicBackgroundDto academicBackground,
      final HealthInclusionDto healthInclusion,
      final ResponsibleDto responsible,
      final PreferenceDto preference,
      final CareerSelectionDto careerSelection,
      final AcademicSpaceSelectionDto academicSpaceSelection,
      final InstrumentSelectionDto instrumentSelection,
      final List<AttachmentDto> attachments) {
    this(
        personalData,
        academicBackground,
        healthInclusion,
        responsible,
        preference,
        careerSelection,
        academicSpaceSelection,
        instrumentSelection,
        new ArrayList<>(),
        attachments);
  }

  public EnrollmentDraftData {
    attachments = attachments == null ? new ArrayList<>() : attachments;
  }

  public PersonalDataDto getPersonalData() {
    return personalData;
  }

  public AcademicBackgroundDto getAcademicBackground() {
    return academicBackground;
  }

  public HealthInclusionDto getHealthInclusion() {
    return healthInclusion;
  }

  public ResponsibleDto getResponsible() {
    return responsible;
  }

  public PreferenceDto getPreference() {
    return preference;
  }

  public CareerSelectionDto getCareerSelection() {
    return careerSelection;
  }

  public AcademicSpaceSelectionDto getAcademicSpaceSelection() {
    return academicSpaceSelection;
  }

  public InstrumentSelectionDto getInstrumentSelection() {
    return instrumentSelection;
  }

  public List<CourseSelectionDto> getCourses() {
    return courses;
  }

  public List<AttachmentDto> getAttachments() {
    return attachments;
  }
}
