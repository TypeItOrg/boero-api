package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDraftData {
  private PersonalDataDto personalData;
  private AcademicBackgroundDto academicBackground;
  private HealthInclusionDto healthInclusion;
  private ResponsibleDto responsible;
  private PreferenceDto preference;
  private CareerSelectionDto careerSelection;
  private AcademicSpaceSelectionDto academicSpaceSelection;
  private InstrumentSelectionDto instrumentSelection;
  @Builder.Default private List<AttachmentDto> attachments = new ArrayList<>();
}
