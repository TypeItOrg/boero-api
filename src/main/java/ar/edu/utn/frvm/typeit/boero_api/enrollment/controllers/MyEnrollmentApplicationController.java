package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresInstitutionAccess;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentApplicationService;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.GetMyEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListMyEnrollmentApplicationsUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/institutions/{institutionId}")
@RequiresInstitutionAccess
@Validated
@RequiredArgsConstructor
public class MyEnrollmentApplicationController {

  private final EnrollmentApplicationService enrollmentApplicationService;
  private final ListMyEnrollmentApplicationsUseCase listMyEnrollmentApplicationsUseCase;
  private final GetMyEnrollmentApplicationUseCase getMyEnrollmentApplicationUseCase;

  @PostMapping(value = "/my-enrollment-applications", version = Version.V1)
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentApplicationResponse start(
      @PathVariable final UUID institutionId,
      @Valid @RequestBody final StartEnrollmentApplicationRequest request,
      final Authentication authentication) {
    return enrollmentApplicationService.startOrGetApplication(
        institutionId, currentPersonId(authentication), request);
  }

  @GetMapping(value = "/my-enrollment-applications", version = Version.V1)
  public PaginatedResponse<EnrollmentApplicationResponse> list(
      @PathVariable final UUID institutionId,
      @RequestParam(required = false) final EnrollmentApplicationStatus status,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable,
      final Authentication authentication) {
    return PaginatedResponse.from(
        listMyEnrollmentApplicationsUseCase.execute(
            institutionId, currentPersonId(authentication), status, pageable));
  }

  @GetMapping(value = "/my-enrollment-applications/{applicationId}", version = Version.V1)
  public EnrollmentApplicationResponse get(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID applicationId,
      final Authentication authentication) {
    return getMyEnrollmentApplicationUseCase.execute(
        institutionId, currentPersonId(authentication), applicationId);
  }

  @PatchMapping(value = "/my-enrollment-applications/{applicationId}", version = Version.V1)
  public EnrollmentApplicationResponse updateDraft(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID applicationId,
      @Valid @RequestBody final UpdateEnrollmentDraftRequest request,
      final Authentication authentication) {
    return enrollmentApplicationService.updateDraft(
        currentPersonId(authentication), applicationId, request);
  }

  @PostMapping(value = "/my-enrollment-applications/{applicationId}/submit", version = Version.V1)
  public EnrollmentApplicationResponse submit(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID applicationId,
      final Authentication authentication) {
    return enrollmentApplicationService.submitApplication(
        currentPersonId(authentication), applicationId);
  }

  private UUID currentPersonId(final Authentication authentication) {
    return ((JwtAuthenticatedUser) authentication.getPrincipal()).personId();
  }
}
