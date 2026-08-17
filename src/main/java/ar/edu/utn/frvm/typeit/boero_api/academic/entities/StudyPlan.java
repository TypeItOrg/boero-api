package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.InvalidAcademicStateException;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "study_plans",
    uniqueConstraints =
        @UniqueConstraint(
            name = "study_plans_institution_id_id_unique",
            columnNames = {"institution_id", "study_plan_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class StudyPlan extends SoftDeletable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "study_plan_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "training_path_id", nullable = false)
  private TrainingPath trainingPath;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StudyPlanStatus status;

  public static StudyPlan create(
      final Institution institution,
      final TrainingPath trainingPath,
      final String name,
      final LocalDate effectiveFrom,
      final LocalDate effectiveTo) {
    validateDates(effectiveFrom, effectiveTo);
    return StudyPlan.builder()
        .institution(institution)
        .trainingPath(trainingPath)
        .name(AcademicNameNormalizer.display(name))
        .effectiveFrom(effectiveFrom)
        .effectiveTo(effectiveTo)
        .status(StudyPlanStatus.DRAFT)
        .build();
  }

  public void updateDraft(
      final String name, final LocalDate effectiveFrom, final LocalDate effectiveTo) {
    ensureDraft();
    validateDates(effectiveFrom, effectiveTo);
    this.name = AcademicNameNormalizer.display(name);
    this.effectiveFrom = effectiveFrom;
    this.effectiveTo = effectiveTo;
  }

  public void activate() {
    if (status != StudyPlanStatus.DRAFT) {
      throw new InvalidAcademicStateException();
    }
    status = StudyPlanStatus.ACTIVE;
  }

  public void deactivate(final LocalDate effectiveTo) {
    if (status != StudyPlanStatus.ACTIVE) {
      throw new InvalidAcademicStateException();
    }
    this.effectiveTo = effectiveTo;
    status = StudyPlanStatus.INACTIVE;
  }

  public void ensureDraft() {
    if (status != StudyPlanStatus.DRAFT) {
      throw new InvalidAcademicStateException();
    }
  }

  public boolean delete(final LocalDateTime deletedAt) {
    ensureDraft();
    return markDeleted(deletedAt);
  }

  private static void validateDates(final LocalDate effectiveFrom, final LocalDate effectiveTo) {
    if (effectiveFrom == null && effectiveTo != null) {
      throw new AcademicValidationException(
          AcademicMessages.STUDY_PLAN_DATES_INVALID,
          Map.of("effectiveFrom", AcademicMessages.STUDY_PLAN_START_DATE_REQUIRED));
    }
    if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
      throw new AcademicValidationException(
          AcademicMessages.STUDY_PLAN_DATES_INVALID,
          Map.of("effectiveTo", AcademicMessages.STUDY_PLAN_DATES_INVALID));
    }
  }
}
