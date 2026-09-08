package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalCallerGuard;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentApplicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identity for every endpoint here comes from the authenticated principal, never from
 * client-supplied headers or parameters — an institutional {@link JwtAuthenticatedUser} always
 * carries its own {@code personId} and {@code institutionId}, so there is nothing for a caller to
 * spoof.
 */
@RestController
@RequestMapping("/enrollment-applications")
@RequiredArgsConstructor
public class EnrollmentApplicationController {

  private final EnrollmentApplicationService applicationService;
  private final InstitutionalCallerGuard institutionalCallerGuard;
  private final AuthorizationService authorizationService;

  @PostMapping(version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> startOrGetApplication(
      Authentication authentication, @Valid @RequestBody StartEnrollmentApplicationRequest request) {
    JwtAuthenticatedUser principal = requireInstitutionalUser(authentication);
    EnrollmentApplicationResponse response =
        applicationService.startOrGetApplication(
            principal.institutionId(), principal.personId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping(value = "/{applicationId}", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> getApplication(
      Authentication authentication, @PathVariable UUID applicationId) {
    JwtAuthenticatedUser principal = requireInstitutionalUser(authentication);
    // Only look up by institution when the caller actually has review permission - otherwise
    // this stays a pure self-service lookup and other applicants' data never matches.
    UUID institutionId =
        authorizationService.hasPermission(authentication, PermissionCode.ENROLLMENT_PERIOD_READ)
            ? principal.institutionId()
            : null;
    EnrollmentApplicationResponse response =
        applicationService.getApplicationById(institutionId, principal.personId(), applicationId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping(value = "/{applicationId}/draft", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> updateDraft(
      Authentication authentication,
      @PathVariable UUID applicationId,
      @RequestBody UpdateEnrollmentDraftRequest request) {
    JwtAuthenticatedUser principal = requireInstitutionalUser(authentication);
    EnrollmentApplicationResponse response =
        applicationService.updateDraft(principal.personId(), applicationId, request);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/{applicationId}/cancel", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> cancelApplication(
      Authentication authentication, @PathVariable UUID applicationId) {
    JwtAuthenticatedUser principal = requireInstitutionalUser(authentication);
    EnrollmentApplicationResponse response =
        applicationService.cancelApplication(principal.personId(), applicationId);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/{applicationId}/submit", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> submitApplication(
      Authentication authentication, @PathVariable UUID applicationId) {
    JwtAuthenticatedUser principal = requireInstitutionalUser(authentication);
    EnrollmentApplicationResponse response =
        applicationService.submitApplication(principal.personId(), applicationId);
    return ResponseEntity.ok(response);
  }

  @GetMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.ENROLLMENT_PERIOD_READ)
  public ResponseEntity<PaginatedResponse<EnrollmentApplicationResponse>> listApplications(
      Authentication authentication,
      @RequestParam(required = false) UUID periodId,
      @RequestParam(required = false) EnrollmentApplicationStatus status,
      @RequestParam(required = false) String search,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    JwtAuthenticatedUser principal = requireInstitutionalUser(authentication);
    PaginatedResponse<EnrollmentApplicationResponse> response =
        applicationService.listApplications(
            principal.institutionId(), periodId, status, search, pageable);
    return ResponseEntity.ok(response);
  }

  private JwtAuthenticatedUser requireInstitutionalUser(Authentication authentication) {
    institutionalCallerGuard.ensureInstitutionalPrincipal(authentication);
    return (JwtAuthenticatedUser) authentication.getPrincipal();
  }
}
