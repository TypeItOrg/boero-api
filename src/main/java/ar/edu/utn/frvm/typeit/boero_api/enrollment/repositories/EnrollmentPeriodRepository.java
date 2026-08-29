package ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentPeriodRepository extends JpaRepository<EnrollmentPeriod, UUID> {

  @Query(
      "SELECT ep FROM EnrollmentPeriod ep WHERE ep.institution.id = :institutionId "
          + "AND ep.academicYear.id = :academicYearId "
          + "AND ep.status = :status "
          + "AND ep.startDate <= :now AND ep.endDate >= :now "
          + "AND ep.deletedAt IS NULL")
  Optional<EnrollmentPeriod> findActivePeriod(
      @Param("institutionId") UUID institutionId,
      @Param("academicYearId") UUID academicYearId,
      @Param("status") EnrollmentPeriodStatus status,
      @Param("now") LocalDateTime now);
}
