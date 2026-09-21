package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollmentApplicationResponseFactory {
  private final EnrollmentApplicationPeriodService periods;

  public EnrollmentApplicationResponse from(final EnrollmentApplication application) {
    return EnrollmentApplicationResponse.from(application).toBuilder()
        .periodOpen(periods.isOpen(application))
        .build();
  }

  public EnrollmentApplicationResponse from(
      final EnrollmentApplication application, final boolean includeCourses) {
    return EnrollmentApplicationResponse.from(application, includeCourses).toBuilder()
        .periodOpen(periods.isOpen(application))
        .build();
  }
}
