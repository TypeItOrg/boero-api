package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalCallerGuard;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CreateEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentApplicationDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.CreateEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.GetEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationStudyPlanSpacesUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationTrainingPathsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.UpdateEnrollmentApplicationDraftUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollment-applications")
@RequiredArgsConstructor
public class EnrollmentApplicationController {

  private final InstitutionalCallerGuard institutionalCallerGuard;
  private final CreateEnrollmentApplicationUseCase createEnrollmentApplicationUseCase;
  private final GetEnrollmentApplicationUseCase getEnrollmentApplicationUseCase;
  private final UpdateEnrollmentApplicationDraftUseCase updateEnrollmentApplicationDraftUseCase;
  private final ListEnrollmentApplicationTrainingPathsUseCase
      listEnrollmentApplicationTrainingPathsUseCase;
  private final ListEnrollmentApplicationStudyPlanSpacesUseCase
      listEnrollmentApplicationStudyPlanSpacesUseCase;

  @PostMapping(version = Version.V1)
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentApplicationResponse create(
      final Authentication authentication,
      @Valid @RequestBody final CreateEnrollmentApplicationRequest request) {
    final var principal = principal(authentication);
    return createEnrollmentApplicationUseCase.execute(principal, request);
  }

  @GetMapping(value = "/{applicationId}", version = Version.V1)
  public EnrollmentApplicationResponse get(
      final Authentication authentication, @PathVariable final UUID applicationId) {
    final var principal = principal(authentication);
    return getEnrollmentApplicationUseCase.execute(principal, applicationId);
  }

  @PatchMapping(value = "/{applicationId}/draft", version = Version.V1)
  public EnrollmentApplicationResponse updateDraft(
      final Authentication authentication,
      @PathVariable final UUID applicationId,
      @Valid @RequestBody final UpdateEnrollmentApplicationDraftRequest request) {
    final var principal = principal(authentication);
    return updateEnrollmentApplicationDraftUseCase.execute(principal, applicationId, request);
  }

  @GetMapping(value = "/{applicationId}/training-paths", version = Version.V1)
  public List<ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TrainingPathResponse>
      listTrainingPaths(
          final Authentication authentication, @PathVariable final UUID applicationId) {
    final var principal = principal(authentication);
    return listEnrollmentApplicationTrainingPathsUseCase.execute(principal, applicationId);
  }

  @GetMapping(value = "/{applicationId}/study-plan-spaces", version = Version.V1)
  public List<StudyPlanSpaceResponse> listStudyPlanSpaces(
      final Authentication authentication, @PathVariable final UUID applicationId) {
    final var principal = principal(authentication);
    return listEnrollmentApplicationStudyPlanSpacesUseCase.execute(principal, applicationId);
  }

  private JwtAuthenticatedUser principal(final Authentication authentication) {
    institutionalCallerGuard.ensureInstitutionalPrincipal(authentication);
    return (JwtAuthenticatedUser) authentication.getPrincipal();
  }
}
