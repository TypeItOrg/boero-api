package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollment-applications")
@RequiredArgsConstructor
public class EnrollmentApplicationController {

  private final EnrollmentApplicationService applicationService;

  @PostMapping(version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> startOrGetApplication(
      @RequestHeader("X-Institution-Id") UUID institutionId,
      @RequestHeader("X-Person-Id") UUID personId,
      @Valid @RequestBody StartEnrollmentApplicationRequest request) {
    EnrollmentApplicationResponse response =
        applicationService.startOrGetApplication(institutionId, personId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping(value = "/{applicationId}", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> getApplication(
      @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
      @RequestHeader(value = "X-Person-Id", required = false) UUID personId,
      @PathVariable UUID applicationId) {
    EnrollmentApplicationResponse response =
        applicationService.getApplicationById(institutionId, personId, applicationId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping(value = "/{applicationId}/draft", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> updateDraft(
      @RequestHeader("X-Person-Id") UUID personId,
      @PathVariable UUID applicationId,
      @RequestBody UpdateEnrollmentDraftRequest request) {
    EnrollmentApplicationResponse response =
        applicationService.updateDraft(personId, applicationId, request);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/{applicationId}/cancel", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> cancelApplication(
      @RequestHeader("X-Person-Id") UUID personId, @PathVariable UUID applicationId) {
    EnrollmentApplicationResponse response =
        applicationService.cancelApplication(personId, applicationId);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/{applicationId}/submit", version = Version.V1)
  public ResponseEntity<EnrollmentApplicationResponse> submitApplication(
      @RequestHeader("X-Person-Id") UUID personId, @PathVariable UUID applicationId) {
    EnrollmentApplicationResponse response =
        applicationService.submitApplication(personId, applicationId);
    return ResponseEntity.ok(response);
  }

  @GetMapping(version = Version.V1)
  public ResponseEntity<PaginatedResponse<EnrollmentApplicationResponse>> listApplications(
      @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId,
      @RequestParam(value = "institutionId", required = false) UUID paramInstitutionId,
      @RequestParam(required = false) UUID periodId,
      @RequestParam(required = false) EnrollmentApplicationStatus status,
      @RequestParam(required = false) String search,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    UUID institutionId = headerInstitutionId != null ? headerInstitutionId : paramInstitutionId;
    if (institutionId == null) {
      throw new IllegalArgumentException(
          "X-Institution-Id header or institutionId parameter is required");
    }
    PaginatedResponse<EnrollmentApplicationResponse> response =
        applicationService.listApplications(institutionId, periodId, status, search, pageable);
    return ResponseEntity.ok(response);
  }
}
