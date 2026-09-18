package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
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
            INSTITUTION_ID, PERSON_ID, pageable))
        .thenReturn(Page.empty(pageable));

    final var response = service.listOwn(INSTITUTION_ID, PERSON_ID, pageable);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isZero();
    verify(courseEnrollmentRepository)
        .findByInstitutionIdAndStudentPersonId(INSTITUTION_ID, PERSON_ID, pageable);
  }
}
