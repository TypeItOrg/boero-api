package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLifecycleEvent;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicLifecycleAction;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicLifecycleResource;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLifecycleEventRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicLifecycleRequest;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.AccountType;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicLifecycleServiceTest {

  @Mock private AcademicYearRepository academicYearRepository;
  @Mock private TrainingPathRepository trainingPathRepository;
  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private AcademicSpaceRepository academicSpaceRepository;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private AcademicLifecycleEventRepository eventRepository;
  @Mock private AcademicLifecycleActorResolver actorResolver;
  @InjectMocks private AcademicLifecycleService service;

  @Test
  void deletesAnInactiveTrainingPathAndRecordsItsActorAndReason() {
    final var institutionId = UUID.randomUUID();
    final var resourceId = UUID.randomUUID();
    final var actorId = UUID.randomUUID();
    final var institution = Institution.builder().id(institutionId).build();
    final var path = TrainingPath.create(institution, "Tecnicatura", null);
    path.updateStatus(false);
    given(trainingPathRepository.findByIdAndInstitution_IdForLifecycle(resourceId, institutionId))
        .willReturn(Optional.of(path));
    given(actorResolver.resolve())
        .willReturn(new AcademicLifecycleActor(AccountType.INSTITUTION, actorId));

    service.deleteTrainingPath(
        institutionId, resourceId, new AcademicLifecycleRequest("  Baja solicitada  "));

    assertThat(path.isDeleted()).isTrue();
    final var eventCaptor = ArgumentCaptor.forClass(AcademicLifecycleEvent.class);
    verify(eventRepository).save(eventCaptor.capture());
    verify(eventRepository).flush();
    assertThat(eventCaptor.getValue())
        .extracting(
            AcademicLifecycleEvent::getResourceType,
            AcademicLifecycleEvent::getResourceId,
            AcademicLifecycleEvent::getAction,
            AcademicLifecycleEvent::getActorType,
            AcademicLifecycleEvent::getActorId,
            AcademicLifecycleEvent::getReason)
        .containsExactly(
            AcademicLifecycleResource.TRAINING_PATH,
            resourceId,
            AcademicLifecycleAction.DELETE,
            AccountType.INSTITUTION,
            actorId,
            "Baja solicitada");
  }

  @Test
  void rejectsDeletingATrainingPathWithCurrentPlansWithoutRecordingAnEvent() {
    final var institutionId = UUID.randomUUID();
    final var resourceId = UUID.randomUUID();
    final var institution = Institution.builder().id(institutionId).build();
    final var path = TrainingPath.create(institution, "Tecnicatura", null);
    path.updateStatus(false);
    given(trainingPathRepository.findByIdAndInstitution_IdForLifecycle(resourceId, institutionId))
        .willReturn(Optional.of(path));
    given(trainingPathRepository.existsCurrentStudyPlan(resourceId)).willReturn(true);

    assertThatThrownBy(() -> service.deleteTrainingPath(institutionId, resourceId, null))
        .isInstanceOf(AcademicConflictException.class);

    assertThat(path.isDeleted()).isFalse();
    verifyNoInteractions(eventRepository, actorResolver);
  }
}
