package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentApplicationDraftRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateEnrollmentApplicationDraftUseCase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final EnrollmentApplicationRepository enrollmentApplicationRepository;
  private final EnrollmentDraftDataValidator enrollmentDraftDataValidator;

  @Transactional
  public EnrollmentApplicationResponse execute(
      final JwtAuthenticatedUser principal,
      final UUID applicationId,
      final UpdateEnrollmentApplicationDraftRequest request) {
    applicantEnrollmentGuard.requireApplicant(principal);
    final var application =
        enrollmentApplicationRepository
            .findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
                applicationId, principal.personId(), principal.institutionId())
            .orElseThrow(EnrollmentApplicationNotFoundException::new);
    enrollmentDraftDataValidator.validate(principal.institutionId(), application, request.data());
    application.replaceDraftData(toPersistenceJson(request.data()));
    return EnrollmentApplicationResponse.from(enrollmentApplicationRepository.save(application));
  }

  private JsonNode toPersistenceJson(final tools.jackson.databind.JsonNode data) {
    try {
      return OBJECT_MAPPER.readTree(data.toString());
    } catch (java.io.IOException exception) {
      throw new IllegalArgumentException("Could not convert enrollment draft data.", exception);
    }
  }
}
