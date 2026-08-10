package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicLevelRepository extends JpaRepository<AcademicLevel, UUID> {
  List<AcademicLevel> findByStudyPlan_IdOrderByDisplayOrderAsc(UUID studyPlanId);

  Optional<AcademicLevel> findByIdAndStudyPlan_Institution_Id(UUID id, UUID institutionId);

  Optional<AcademicLevel> findByIdAndStudyPlan_Id(UUID id, UUID studyPlanId);

  boolean existsByStudyPlan_IdAndDisplayOrder(UUID studyPlanId, int displayOrder);

  boolean existsByStudyPlan_IdAndDisplayOrderAndIdNot(UUID studyPlanId, int displayOrder, UUID id);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM academic_levels WHERE study_plan_id = :studyPlanId AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedName(
      @Param("studyPlanId") UUID studyPlanId, @Param("name") String name);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM academic_levels WHERE study_plan_id = :studyPlanId AND academic_level_id <> :id AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedNameAndIdNot(
      @Param("studyPlanId") UUID studyPlanId, @Param("name") String name, @Param("id") UUID id);
}
