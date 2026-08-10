package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.ApprovalMode;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequiredCondition;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementStage;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementType;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.time.LocalDate;
import java.util.UUID;

public final class AcademicEntityTestFactory {

  private AcademicEntityTestFactory() {}

  public static StudyPlan studyPlan(final Institution institution) {
    return StudyPlan.builder()
        .id(UUID.randomUUID())
        .institution(institution)
        .trainingPath(TrainingPath.create(institution, "Path", null))
        .name("Plan")
        .effectiveFrom(LocalDate.of(2026, 1, 1))
        .effectiveTo(LocalDate.of(2030, 12, 31))
        .status(StudyPlanStatus.DRAFT)
        .build();
  }

  public static StudyPlanSpace studyPlanSpace(final StudyPlan plan, final UUID id) {
    final var academicSpace =
        AcademicSpace.builder()
            .id(UUID.randomUUID())
            .institution(plan.getInstitution())
            .name(id.toString())
            .type(AcademicSpaceType.SUBJECT)
            .active(true)
            .build();
    return StudyPlanSpace.builder()
        .id(id)
        .institution(plan.getInstitution())
        .studyPlan(plan)
        .academicSpace(academicSpace)
        .requirementType(RequirementType.REQUIRED)
        .displayOrder(1)
        .approvalMode(ApprovalMode.PROMOTION)
        .build();
  }

  public static Prerequisite prerequisite(
      final StudyPlan plan, final StudyPlanSpace target, final StudyPlanSpace required) {
    return Prerequisite.builder()
        .id(UUID.randomUUID())
        .studyPlan(plan)
        .targetStudyPlanSpace(target)
        .requiredStudyPlanSpace(required)
        .requirementStage(RequirementStage.TO_ENROLL)
        .requiredCondition(RequiredCondition.PASSED)
        .build();
  }
}
