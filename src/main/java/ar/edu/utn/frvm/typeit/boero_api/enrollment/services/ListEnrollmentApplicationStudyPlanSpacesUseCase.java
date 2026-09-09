package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceInstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceInstrumentOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListEnrollmentApplicationStudyPlanSpacesUseCase {

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final EnrollmentApplicationRepository enrollmentApplicationRepository;
  private final EnrollmentEffectiveStudyPlanResolver enrollmentEffectiveStudyPlanResolver;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final StudyPlanSpaceInstrumentRepository studyPlanSpaceInstrumentRepository;

  @Transactional(readOnly = true)
  public List<StudyPlanSpaceResponse> execute(
      final JwtAuthenticatedUser principal, final UUID applicationId) {
    applicantEnrollmentGuard.requireApplicant(principal);
    final var application =
        enrollmentApplicationRepository
            .findById(applicationId)
            .filter(app -> app.getDeletedAt() == null)
            .filter(app -> app.getApplicantPerson().getId().equals(principal.personId()))
            .filter(app -> app.getInstitution().getId().equals(principal.institutionId()))
            .orElseThrow(EnrollmentApplicationNotFoundException::new);
    final var effectiveStudyPlan =
        enrollmentEffectiveStudyPlanResolver.resolve(principal.institutionId(), application);
    final var studyPlanSpaces =
        studyPlanSpaceRepository.findEligibleByStudyPlanId(
            principal.institutionId(), effectiveStudyPlan.getId());
    final var allowedInstrumentsByStudyPlanSpaceId =
        studyPlanSpaceInstrumentRepository
            .findActiveByStudyPlanSpaceIds(
                principal.institutionId(),
                studyPlanSpaces.stream().map(space -> space.getId()).toList())
            .stream()
            .collect(
                Collectors.groupingBy(
                    relation -> relation.getStudyPlanSpace().getId(),
                    Collectors.mapping(
                        relation ->
                            new StudyPlanSpaceInstrumentOptionResponse(
                                relation.getInstrument().getId(),
                                relation.getInstrument().getName()),
                        Collectors.toList())));

    return studyPlanSpaces.stream()
        .map(
            studyPlanSpace ->
                StudyPlanSpaceResponse.from(
                    studyPlanSpace,
                    allowedInstrumentsByStudyPlanSpaceId.getOrDefault(
                        studyPlanSpace.getId(), List.of())))
        .toList();
  }
}
