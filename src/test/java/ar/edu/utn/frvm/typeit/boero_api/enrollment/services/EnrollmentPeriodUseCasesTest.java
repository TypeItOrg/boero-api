package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentPeriodDatesException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CreateEnrollmentPeriodRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentPeriodStatusRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentPeriodRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentPeriodUseCasesTest {

  @Mock private EnrollmentPeriodRepository periodRepository;
  @Mock private InstitutionRepository institutionRepository;
  @Mock private AcademicYearRepository academicYearRepository;

  private CreateEnrollmentPeriodUseCase createUseCase;
  private GetEnrollmentPeriodUseCase getUseCase;
  private UpdateEnrollmentPeriodUseCase updateUseCase;
  private UpdateEnrollmentPeriodStatusUseCase updateStatusUseCase;
  private DeleteEnrollmentPeriodUseCase deleteUseCase;

  private UUID institutionId;
  private UUID academicYearId;
  private UUID periodId;
  private Institution institution;
  private AcademicYear academicYear;
  private EnrollmentPeriod period;

  @BeforeEach
  void setUp() {
    createUseCase =
        new CreateEnrollmentPeriodUseCase(
            periodRepository, institutionRepository, academicYearRepository);
    getUseCase = new GetEnrollmentPeriodUseCase(periodRepository);
    updateUseCase = new UpdateEnrollmentPeriodUseCase(periodRepository);
    updateStatusUseCase = new UpdateEnrollmentPeriodStatusUseCase(periodRepository);
    deleteUseCase = new DeleteEnrollmentPeriodUseCase(periodRepository);

    institutionId = UUID.randomUUID();
    academicYearId = UUID.randomUUID();
    periodId = UUID.randomUUID();

    institution = Institution.builder().id(institutionId).name("UTN FRVM").build();
    academicYear = AcademicYear.create(institution, 2026, null, null);

    period =
        EnrollmentPeriod.builder()
            .id(periodId)
            .institution(institution)
            .academicYear(academicYear)
            .name("Inscripción 2026 - Primer Llamado")
            .startDate(LocalDateTime.of(2026, 11, 1, 8, 0))
            .endDate(LocalDateTime.of(2026, 12, 1, 20, 0))
            .status(EnrollmentPeriodStatus.PLANNED)
            .build();
  }

  @Test
  @DisplayName("Create: Debería crear un período exitosamente")
  void create_success() {
    var request =
        new CreateEnrollmentPeriodRequest(
            academicYearId,
            "Inscripción 2026",
            LocalDateTime.of(2026, 11, 1, 8, 0),
            LocalDateTime.of(2026, 12, 1, 20, 0));

    when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear));
    when(periodRepository.save(any())).thenReturn(period);

    var response = createUseCase.execute(institutionId, request);

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Inscripción 2026 - Primer Llamado");
    assertThat(response.status()).isEqualTo(EnrollmentPeriodStatus.PLANNED);
  }

  @Test
  @DisplayName("Create: Debería fallar si las fechas son inválidas")
  void create_invalidDates() {
    var request =
        new CreateEnrollmentPeriodRequest(
            academicYearId,
            "Inscripción 2026",
            LocalDateTime.of(2026, 12, 1, 8, 0),
            LocalDateTime.of(2026, 11, 1, 20, 0));

    assertThatThrownBy(() -> createUseCase.execute(institutionId, request))
        .isInstanceOf(InvalidEnrollmentPeriodDatesException.class);
  }

  @Test
  @DisplayName("Create: Debería fallar si la institución no existe")
  void create_institutionNotFound() {
    var request =
        new CreateEnrollmentPeriodRequest(
            academicYearId,
            "Inscripción 2026",
            LocalDateTime.of(2026, 11, 1, 8, 0),
            LocalDateTime.of(2026, 12, 1, 20, 0));

    when(institutionRepository.findById(institutionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createUseCase.execute(institutionId, request))
        .isInstanceOf(InstitutionNotFoundException.class);
  }

  @Test
  @DisplayName("Create: Debería fallar si el ciclo lectivo no existe")
  void create_academicYearNotFound() {
    var request =
        new CreateEnrollmentPeriodRequest(
            academicYearId,
            "Inscripción 2026",
            LocalDateTime.of(2026, 11, 1, 8, 0),
            LocalDateTime.of(2026, 12, 1, 20, 0));

    when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createUseCase.execute(institutionId, request))
        .isInstanceOf(AcademicYearNotFoundException.class);
  }

  @Test
  @DisplayName("Get: Debería devolver el detalle del período")
  void get_success() {
    when(periodRepository.findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId))
        .thenReturn(Optional.of(period));

    var response = getUseCase.execute(institutionId, periodId);

    assertThat(response.id()).isEqualTo(periodId);
    assertThat(response.name()).isEqualTo(period.getName());
  }

  @Test
  @DisplayName("Get: Debería fallar si el período no existe")
  void get_notFound() {
    when(periodRepository.findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> getUseCase.execute(institutionId, periodId))
        .isInstanceOf(EnrollmentPeriodNotFoundException.class);
  }

  @Test
  @DisplayName("Update: Debería actualizar datos correctamente")
  void update_success() {
    var request =
        new UpdateEnrollmentPeriodRequest(
            "Inscripción 2026 Editada",
            LocalDateTime.of(2026, 11, 5, 8, 0),
            LocalDateTime.of(2026, 12, 10, 20, 0));

    when(periodRepository.findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId))
        .thenReturn(Optional.of(period));
    when(periodRepository.save(any())).thenReturn(period);

    var response = updateUseCase.execute(institutionId, periodId, request);

    assertThat(response).isNotNull();
    verify(periodRepository).save(period);
  }

  @Test
  @DisplayName("UpdateStatus: Debería cambiar el estado correctamente")
  void updateStatus_success() {
    var request = new EnrollmentPeriodStatusRequest(EnrollmentPeriodStatus.OPEN);

    when(periodRepository.findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId))
        .thenReturn(Optional.of(period));

    updateStatusUseCase.execute(institutionId, periodId, request);

    assertThat(period.getStatus()).isEqualTo(EnrollmentPeriodStatus.OPEN);
    verify(periodRepository).save(period);
  }

  @Test
  @DisplayName("Delete: Debería marcar como eliminado (soft delete)")
  void delete_success() {
    when(periodRepository.findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId))
        .thenReturn(Optional.of(period));

    deleteUseCase.execute(institutionId, periodId);

    assertThat(period.getDeletedAt()).isNotNull();
    verify(periodRepository).save(period);
  }
}
