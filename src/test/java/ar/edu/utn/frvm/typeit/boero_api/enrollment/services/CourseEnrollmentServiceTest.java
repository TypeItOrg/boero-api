package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseEnrollmentScheduleRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentServiceTest {

  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID PERSON_ID = UUID.randomUUID();

  @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
  @Mock private CourseEnrollmentScheduleRepository courseEnrollmentScheduleRepository;

  @InjectMocks private CourseEnrollmentService service;

  @Test
  @DisplayName("Should return an empty page when the person has no student record")
  void listOwn_returnsEmptyPageWithoutStudentRecord() {
    final Pageable pageable = PageRequest.of(0, 20);
    when(courseEnrollmentRepository.findByInstitutionIdAndStudentPersonId(
            INSTITUTION_ID, PERSON_ID, null, null, pageable))
        .thenReturn(Page.empty(pageable));

    final var response = service.listOwn(INSTITUTION_ID, PERSON_ID, null, null, pageable);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isZero();
    verify(courseEnrollmentRepository)
        .findByInstitutionIdAndStudentPersonId(INSTITUTION_ID, PERSON_ID, null, null, pageable);
  }

  @Test
  @DisplayName("Should forward status filters when listing own enrollments")
  void listOwn_forwardsStatusFilters() {
    final Pageable pageable = PageRequest.of(0, 20);
    when(courseEnrollmentRepository.findByInstitutionIdAndStudentPersonId(
            eq(INSTITUTION_ID),
            eq(PERSON_ID),
            eq(ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus.ENROLLED),
            eq(
                ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus
                    .IN_PROGRESS),
            eq(pageable)))
        .thenReturn(Page.empty(pageable));

    final var response =
        service.listOwn(
            INSTITUTION_ID,
            PERSON_ID,
            ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus.ENROLLED,
            ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus.IN_PROGRESS,
            pageable);

    assertThat(response.items()).isEmpty();
  }
}
