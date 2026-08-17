package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingPathRepository extends JpaRepository<TrainingPath, UUID> {
  @Query(
      """
      SELECT path FROM TrainingPath path
      WHERE path.institution.id = :institutionId
        AND ((:deleted = true AND path.deletedAt IS NOT NULL) OR (:deleted = false AND path.deletedAt IS NULL))
        AND (:active IS NULL OR path.active = :active)
        AND (:search IS NULL OR UNACCENT_LOWER(path.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      """)
  Page<TrainingPath> findByFilters(
      @Param("institutionId") UUID institutionId,
      @Param("search") String search,
      @Param("active") Boolean active,
      @Param("deleted") boolean deleted,
      Pageable pageable);

  @Query(
      "SELECT path FROM TrainingPath path WHERE path.id = :id AND path.institution.id = :institutionId AND path.deletedAt IS NULL")
  Optional<TrainingPath> findByIdAndInstitution_Id(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  Optional<TrainingPath> findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(
      UUID id, UUID institutionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT path FROM TrainingPath path WHERE path.id = :id AND path.institution.id = :institutionId")
  Optional<TrainingPath> findByIdAndInstitution_IdForLifecycle(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM training_paths WHERE institution_id = :institutionId AND deleted_at IS NULL AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedName(
      @Param("institutionId") UUID institutionId, @Param("name") String name);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM training_paths WHERE institution_id = :institutionId AND deleted_at IS NULL AND training_path_id <> :id AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedNameAndIdNot(
      @Param("institutionId") UUID institutionId, @Param("name") String name, @Param("id") UUID id);

  @Query(
      "SELECT COUNT(plan) > 0 FROM StudyPlan plan WHERE plan.trainingPath.id = :trainingPathId AND plan.deletedAt IS NULL AND plan.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus.ACTIVE")
  boolean existsActiveStudyPlan(@Param("trainingPathId") UUID trainingPathId);

  @Query(
      "SELECT COUNT(plan) > 0 FROM StudyPlan plan WHERE plan.trainingPath.id = :trainingPathId AND plan.deletedAt IS NULL")
  boolean existsCurrentStudyPlan(@Param("trainingPathId") UUID trainingPathId);
}
