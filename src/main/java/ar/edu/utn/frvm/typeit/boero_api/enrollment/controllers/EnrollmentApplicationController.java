package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentApplicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollment-applications")
@RequiredArgsConstructor
public class EnrollmentApplicationController {

  private final EnrollmentApplicationService applicationService;

  @PostMapping
  public ResponseEntity<EnrollmentApplicationResponse> startOrGetApplication(
      @RequestHeader("X-Institution-Id") UUID institutionId,
      @RequestHeader("X-Person-Id") UUID personId,
      @Valid @RequestBody StartEnrollmentApplicationRequest request) {
    EnrollmentApplicationResponse response =
        applicationService.startOrGetApplication(institutionId, personId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{applicationId}")
  public ResponseEntity<EnrollmentApplicationResponse> getApplication(
      @RequestHeader("X-Person-Id") UUID personId, @PathVariable UUID applicationId) {
    EnrollmentApplicationResponse response =
        applicationService.getApplicationById(personId, applicationId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{applicationId}/draft")
  public ResponseEntity<EnrollmentApplicationResponse> updateDraft(
      @RequestHeader("X-Person-Id") UUID personId,
      @PathVariable UUID applicationId,
      @RequestBody UpdateEnrollmentDraftRequest request) {
    EnrollmentApplicationResponse response =
        applicationService.updateDraft(personId, applicationId, request);
    return ResponseEntity.ok(response);
  }
}
