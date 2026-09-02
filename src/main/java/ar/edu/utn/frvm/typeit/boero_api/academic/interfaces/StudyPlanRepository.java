package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyPlanRepository
    extends JpaRepository<StudyPlan, UUID>, JpaSpecificationExecutor<StudyPlan> {
  @EntityGraph(attributePaths = {"institution", "trainingPath"})
  Page<StudyPlan> findAll(Specification<StudyPlan> specification, Pageable pageable);

  @EntityGraph(attributePaths = {"institution", "trainingPath"})
  Page<StudyPlan> findByTrainingPath_IdAndInstitution_IdAndDeletedAtIsNull(
      UUID trainingPathId, UUID institutionId, Pageable pageable);

  @EntityGraph(attributePaths = {"institution", "trainingPath"})
  @Query(
      """
      SELECT plan FROM StudyPlan plan
      WHERE plan.trainingPath.id = :trainingPathId
        AND plan.institution.id = :institutionId
        AND plan.status = :status
        AND plan.deletedAt IS NULL
        AND (plan.effectiveFrom IS NULL OR plan.effectiveFrom <= :validOn)
        AND (plan.effectiveTo IS NULL OR plan.effectiveTo >= :validOn)
      ORDER BY plan.effectiveFrom DESC, plan.createdAt DESC
      """)
  java.util.List<StudyPlan> findActiveByTrainingPathIdAndInstitutionIdValidOn(
      @Param("trainingPathId") UUID trainingPathId,
      @Param("institutionId") UUID institutionId,
      @Param("status") StudyPlanStatus status,
      @Param("validOn") LocalDate validOn);

  @EntityGraph(attributePaths = {"institution", "trainingPath"})
  @Query(
      """
      SELECT plan FROM StudyPlan plan
      WHERE plan.institution.id = :institutionId
        AND plan.deletedAt IS NULL
        AND EXISTS (
          SELECT space.id FROM StudyPlanSpace space
          WHERE space.studyPlan.id = plan.id
            AND space.institution.id = :institutionId
            AND space.academicSpace.id = :academicSpaceId
        )
      """)
  Page<StudyPlan> findByAcademicSpaceIdAndInstitutionId(
      @Param("institutionId") UUID institutionId,
      @Param("academicSpaceId") UUID academicSpaceId,
      Pageable pageable);

  @EntityGraph(attributePaths = {"institution", "trainingPath"})
  @Query(
      "SELECT plan FROM StudyPlan plan WHERE plan.id = :id AND plan.institution.id = :institutionId AND plan.deletedAt IS NULL")
  Optional<StudyPlan> findByIdAndInstitution_Id(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT plan FROM StudyPlan plan WHERE plan.id = :id AND plan.institution.id = :institutionId AND plan.deletedAt IS NULL")
  Optional<StudyPlan> findByIdAndInstitution_IdForUpdate(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT plan FROM StudyPlan plan WHERE plan.id = :id AND plan.institution.id = :institutionId")
  Optional<StudyPlan> findByIdAndInstitution_IdForLifecycle(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM study_plans WHERE training_path_id = :trainingPathId AND deleted_at IS NULL AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedName(
      @Param("trainingPathId") UUID trainingPathId, @Param("name") String name);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM study_plans WHERE training_path_id = :trainingPathId AND deleted_at IS NULL AND study_plan_id <> :id AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedNameAndIdNot(
      @Param("trainingPathId") UUID trainingPathId,
      @Param("name") String name,
      @Param("id") UUID id);
}
