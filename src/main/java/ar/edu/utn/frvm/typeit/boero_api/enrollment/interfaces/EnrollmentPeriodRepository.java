package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentPeriodRepository extends JpaRepository<EnrollmentPeriod, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT period FROM EnrollmentPeriod period "
          + "WHERE period.institution.id = :institutionId "
          + "AND period.academicYear.id = :academicYearId "
          + "AND period.status = :status "
          + "AND period.deletedAt IS NULL")
  Optional<EnrollmentPeriod> findByInstitutionIdAndAcademicYearIdAndStatusForUpdate(
      @Param("institutionId") UUID institutionId,
      @Param("academicYearId") UUID academicYearId,
      @Param("status") EnrollmentPeriodStatus status);
}
