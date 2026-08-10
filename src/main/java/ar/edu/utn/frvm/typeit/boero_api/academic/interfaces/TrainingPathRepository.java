package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingPathRepository extends JpaRepository<TrainingPath, UUID> {
  @Query(
      """
      SELECT path FROM TrainingPath path
      WHERE path.institution.id = :institutionId
        AND (:active IS NULL OR path.active = :active)
        AND (:search IS NULL OR UNACCENT_LOWER(path.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      """)
  Page<TrainingPath> findByFilters(
      @Param("institutionId") UUID institutionId,
      @Param("search") String search,
      @Param("active") Boolean active,
      Pageable pageable);

  Optional<TrainingPath> findByIdAndInstitution_Id(UUID id, UUID institutionId);

  Optional<TrainingPath> findByIdAndInstitution_IdAndActiveTrue(UUID id, UUID institutionId);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM training_paths WHERE institution_id = :institutionId AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedName(
      @Param("institutionId") UUID institutionId, @Param("name") String name);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM training_paths WHERE institution_id = :institutionId AND training_path_id <> :id AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedNameAndIdNot(
      @Param("institutionId") UUID institutionId, @Param("name") String name, @Param("id") UUID id);

  @Query(
      "SELECT COUNT(plan) > 0 FROM StudyPlan plan WHERE plan.trainingPath.id = :trainingPathId AND plan.status = ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus.ACTIVE")
  boolean existsActiveStudyPlan(@Param("trainingPathId") UUID trainingPathId);
}
