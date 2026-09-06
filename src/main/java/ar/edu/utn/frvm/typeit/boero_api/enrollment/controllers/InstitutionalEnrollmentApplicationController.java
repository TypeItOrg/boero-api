package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresInstitutionAccess;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.RejectEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ApproveEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.GetEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.RejectEnrollmentApplicationUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/institutions/{institutionId}/enrollment-applications")
@RequiresInstitutionAccess
@Validated
@RequiredArgsConstructor
public class InstitutionalEnrollmentApplicationController {

  private final ListEnrollmentApplicationsUseCase listEnrollmentApplicationsUseCase;
  private final GetEnrollmentApplicationUseCase getEnrollmentApplicationUseCase;
  private final ApproveEnrollmentApplicationUseCase approveEnrollmentApplicationUseCase;
  private final RejectEnrollmentApplicationUseCase rejectEnrollmentApplicationUseCase;

  @GetMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.ENROLLMENT_APPLICATION_READ)
  public PaginatedResponse<EnrollmentApplicationResponse> list(
      @PathVariable final UUID institutionId,
      @RequestParam(required = false) final EnrollmentApplicationStatus status,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
          final Pageable pageable) {
    return PaginatedResponse.from(
        listEnrollmentApplicationsUseCase.execute(institutionId, status, pageable));
  }

  @GetMapping(value = "/{applicationId}", version = Version.V1)
  @RequiresPermission(PermissionCode.ENROLLMENT_APPLICATION_READ)
  public EnrollmentApplicationResponse get(
      @PathVariable final UUID institutionId, @PathVariable final UUID applicationId) {
    return getEnrollmentApplicationUseCase.execute(institutionId, applicationId);
  }

  @PostMapping(value = "/{applicationId}/approve", version = Version.V1)
  @RequiresPermission(PermissionCode.ENROLLMENT_APPLICATION_APPROVE)
  public EnrollmentApplicationResponse approve(
      @PathVariable final UUID institutionId, @PathVariable final UUID applicationId) {
    return approveEnrollmentApplicationUseCase.execute(institutionId, applicationId);
  }

  @PostMapping(value = "/{applicationId}/reject", version = Version.V1)
  @RequiresPermission(PermissionCode.ENROLLMENT_APPLICATION_REJECT)
  public EnrollmentApplicationResponse reject(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID applicationId,
      @Valid @RequestBody final RejectEnrollmentApplicationRequest request) {
    return rejectEnrollmentApplicationUseCase.execute(institutionId, applicationId, request);
  }
}
