package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicSpaceRepository extends JpaRepository<AcademicSpace, UUID> {
  @Query(
      """
      SELECT space FROM AcademicSpace space
      WHERE space.institution.id = :institutionId
        AND (:active IS NULL OR space.active = :active)
        AND (:type IS NULL OR space.type = :type)
        AND (:search IS NULL OR UNACCENT_LOWER(space.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      """)
  Page<AcademicSpace> findByFilters(
      @Param("institutionId") UUID institutionId,
      @Param("search") String search,
      @Param("active") Boolean active,
      @Param("type") AcademicSpaceType type,
      Pageable pageable);

  Optional<AcademicSpace> findByIdAndInstitution_Id(UUID id, UUID institutionId);

  Optional<AcademicSpace> findByIdAndInstitution_IdAndActiveTrue(UUID id, UUID institutionId);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM academic_spaces WHERE institution_id = :institutionId AND type = :type AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedNameAndType(
      @Param("institutionId") UUID institutionId,
      @Param("name") String name,
      @Param("type") String type);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM academic_spaces WHERE institution_id = :institutionId AND academic_space_id <> :id AND type = :type AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedNameAndTypeAndIdNot(
      @Param("institutionId") UUID institutionId,
      @Param("name") String name,
      @Param("type") String type,
      @Param("id") UUID id);

  @Query(
      "SELECT COUNT(space) > 0 FROM StudyPlanSpace space WHERE space.academicSpace.id = :academicSpaceId AND space.studyPlan.status IN (ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus.DRAFT, ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus.ACTIVE)")
  boolean existsInEditableOrActivePlan(@Param("academicSpaceId") UUID academicSpaceId);
}
