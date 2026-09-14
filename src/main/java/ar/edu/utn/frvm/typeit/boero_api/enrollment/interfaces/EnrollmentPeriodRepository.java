package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentPeriodRepository
    extends JpaRepository<EnrollmentPeriod, UUID>, JpaSpecificationExecutor<EnrollmentPeriod> {

  Optional<EnrollmentPeriod> findByIdAndInstitutionIdAndDeletedAtIsNull(
      UUID id, UUID institutionId);

  @Query(
      "SELECT ep FROM EnrollmentPeriod ep WHERE ep.institution.id = :institutionId "
          + "AND ep.academicYear.id = :academicYearId "
          + "AND ep.status = :status "
          + "AND ep.startDate <= :now AND ep.endDate >= :now "
          + "AND ep.deletedAt IS NULL ORDER BY ep.startDate DESC, ep.id DESC")
  List<EnrollmentPeriod> findActivePeriods(
      @Param("institutionId") UUID institutionId,
      @Param("academicYearId") UUID academicYearId,
      @Param("status") EnrollmentPeriodStatus status,
      @Param("now") Instant now,
      Pageable pageable);

  @Query(
      "SELECT ep FROM EnrollmentPeriod ep WHERE ep.institution.id = :institutionId "
          + "AND ep.status = :status "
          + "AND ep.startDate <= :now AND ep.endDate >= :now "
          + "AND ep.deletedAt IS NULL")
  Page<EnrollmentPeriod> findAvailableForEnrollment(
      @Param("institutionId") UUID institutionId,
      @Param("status") EnrollmentPeriodStatus status,
      @Param("now") Instant now,
      Pageable pageable);

  default Optional<EnrollmentPeriod> findActivePeriod(
      UUID institutionId, UUID academicYearId, EnrollmentPeriodStatus status, Instant now) {
    return findActivePeriods(institutionId, academicYearId, status, now, Pageable.ofSize(1))
        .stream()
        .findFirst();
  }
}
