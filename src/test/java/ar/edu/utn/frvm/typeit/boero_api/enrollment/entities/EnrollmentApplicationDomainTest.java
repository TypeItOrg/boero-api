package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnrollmentApplicationDomainTest {

  @Test
  @DisplayName("Should report draft applications as editable")
  void reportsDraftAsEditable() {
    final var application = application();

    assertThat(application.isEditable()).isTrue();
  }

  @Test
  @DisplayName("Should report non-draft applications as not editable")
  void reportsNonDraftAsNotEditable() {
    final var application = application();
    application.setStatus(EnrollmentApplicationStatus.CANCELLED);

    assertThat(application.isEditable()).isFalse();
  }

  @Test
  @DisplayName("Should manage selected spaces collection")
  void managesSelectedSpaces() {
    final var application = application();
    final var space = org.mockito.Mockito.mock(StudyPlanSpace.class);
    final var selectedSpace =
        EnrollmentApplicationSpace.builder()
            .enrollmentApplication(application)
            .studyPlanSpace(space)
            .build();

    application.addSelectedSpace(selectedSpace);
    assertThat(application.getSelectedSpaces()).hasSize(1);

    application.clearSelectedSpaces();
    assertThat(application.getSelectedSpaces()).isEmpty();
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
        AcademicYear.create(
            institution,
            2026,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 1, 1));
    return EnrollmentApplication.builder()
        .institution(institution)
        .applicantPerson(applicant)
        .studyPlan(plan)
        .academicYear(year)
        .status(EnrollmentApplicationStatus.DRAFT)
        .build();
  }
}
