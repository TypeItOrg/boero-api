package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentPeriodStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateEnrollmentPeriodStatusUseCase {
  private final EnrollmentPeriodAccessService periodAccess;

  private final EnrollmentPeriodRepository periodRepository;
  private final EnrollmentInstitutionLock institutionLock;
  private final EnrollmentPeriodScopeService scopeService;

  @Transactional
  public void execute(
      final UUID institutionId, final UUID periodId, final EnrollmentPeriodStatusRequest request) {
    institutionLock.lock(institutionId);

    final var period =
        periodRepository
            .findByIdAndInstitutionIdAndDeletedAtIsNull(periodId, institutionId)
            .orElseThrow(EnrollmentPeriodNotFoundException::new);
    periodAccess.requireManage(period, PermissionCode.ENROLLMENT_PERIOD_STATUS_UPDATE);

    if (request.status() == EnrollmentPeriodStatus.OPEN && !period.isScopeConfigured()) {
      throw new ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions
          .EnrollmentValidationException(EnrollmentMessages.PERIOD_SCOPE_REQUIRED);
    }
    period.changeStatus(request.status());
    scopeService.save(period);
  }
}
