package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnrollmentApplicationDomainTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  @DisplayName("Should report draft applications as editable")
  void reportsDraftAsEditable() {
    final var application = application();

    assertThat(application.isEditable()).isTrue();
  }

  @Test
  @DisplayName("Should reject replacing draft data when the application is no longer editable")
  void rejectsReplacingDataWhenNotEditable() {
    final var application = application();
    application.cancel();

    assertThatThrownBy(() -> application.replaceDraftData(OBJECT_MAPPER.createObjectNode()))
        .isInstanceOf(EnrollmentApplicationNotEditableException.class);
  }

  private static EnrollmentApplication application() {
    final var institution = Institution.builder().id(UUID.randomUUID()).build();
    final var applicant =
        Person.builder()
            .id(UUID.randomUUID())
            .institution(institution)
            .firstName("Ana")
            .lastName("Garcia")
            .documentNumber("12345678")
            .email("ana@example.com")
            .build();
    final var path = TrainingPath.create(institution, "Trayecto", null);
    final StudyPlan plan =
        StudyPlan.create(institution, path, "Plan", LocalDate.of(2026, 3, 1), null);
    final AcademicYear year =
        AcademicYear.create(institution, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 1));
    return EnrollmentApplication.create(applicant, institution, plan, year, null);
  }
}
