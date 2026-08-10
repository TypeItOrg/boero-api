package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyPlanSpaceRepository extends JpaRepository<StudyPlanSpace, UUID> {
  @Query(
      """
      SELECT space FROM StudyPlanSpace space
      LEFT JOIN FETCH space.academicLevel level
      JOIN FETCH space.academicSpace academicSpace
      WHERE space.studyPlan.id = :studyPlanId
      ORDER BY level.displayOrder NULLS LAST, space.displayOrder
      """)
  List<StudyPlanSpace> findByStudyPlanIdWithDetails(@Param("studyPlanId") UUID studyPlanId);

  @Query(
      """
      SELECT space FROM StudyPlanSpace space
      LEFT JOIN FETCH space.academicLevel
      JOIN FETCH space.academicSpace
      WHERE space.id = :id
        AND space.institution.id = :institutionId
      """)
  Optional<StudyPlanSpace> findDetailsByIdAndInstitutionId(
      @Param("id") UUID id, @Param("institutionId") UUID institutionId);

  Optional<StudyPlanSpace> findByIdAndInstitution_Id(UUID id, UUID institutionId);

  boolean existsByAcademicLevel_Id(UUID academicLevelId);

  @Query(
      "SELECT COUNT(space) > 0 FROM StudyPlanSpace space WHERE space.studyPlan.id = :studyPlanId")
  boolean existsByStudyPlanId(@Param("studyPlanId") UUID studyPlanId);
}
