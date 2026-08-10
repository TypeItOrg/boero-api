package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.InvalidAcademicStateException;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "academic_years",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "academic_years_institution_id_id_unique",
          columnNames = {"institution_id", "academic_year_id"}),
      @UniqueConstraint(
          name = "academic_years_institution_year_unique",
          columnNames = {"institution_id", "year"})
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AcademicYear extends Auditable {

  public static final int MIN_YEAR = 2000;

  private static final int MAX_YEAR_OFFSET = 1;
  private static final ZoneId ARGENTINA_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  @Id
  @GeneratedUUIDv7
  @Column(name = "academic_year_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @Column(nullable = false)
  private int year;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AcademicYearStatus status;

  public static AcademicYear create(
      final Institution institution,
      final int year,
      final LocalDate startDate,
      final LocalDate endDate) {
    validate(year, startDate, endDate);
    return AcademicYear.builder()
        .institution(institution)
        .year(year)
        .startDate(startDate)
        .endDate(endDate)
        .status(AcademicYearStatus.PLANNED)
        .build();
  }

  public void update(final int year, final LocalDate startDate, final LocalDate endDate) {
    if (status != AcademicYearStatus.PLANNED) {
      throw new InvalidAcademicStateException();
    }
    validate(year, startDate, endDate);
    this.year = year;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public void transitionTo(final AcademicYearStatus target) {
    if (target == status) {
      return;
    }
    final boolean validTransition =
        (status == AcademicYearStatus.PLANNED && target == AcademicYearStatus.ACTIVE)
            || (status == AcademicYearStatus.ACTIVE && target == AcademicYearStatus.CLOSED);
    if (!validTransition) {
      throw new InvalidAcademicStateException();
    }
    status = target;
  }

  private static void validate(final int year, final LocalDate startDate, final LocalDate endDate) {
    validateYear(year);

    final boolean onlyOneDate = (startDate == null) != (endDate == null);
    if (onlyOneDate) {
      throw dateValidationError("endDate", AcademicMessages.DATE_PAIR_REQUIRED);
    }
    if (startDate != null && startDate.isAfter(endDate)) {
      throw dateValidationError("endDate", AcademicMessages.ACADEMIC_YEAR_DATES_INVALID);
    }
    if (startDate == null) {
      return;
    }
    if (startDate.getYear() != year) {
      throw dateValidationError("startDate", AcademicMessages.ACADEMIC_YEAR_START_DATE_INVALID);
    }
    final int endYear = endDate.getYear();
    if (endYear != year && endYear != year + 1) {
      throw dateValidationError("endDate", AcademicMessages.ACADEMIC_YEAR_END_DATE_INVALID);
    }
  }

  private static void validateYear(final int year) {
    final int maxYear = Year.now(ARGENTINA_TIME_ZONE).getValue() + MAX_YEAR_OFFSET;
    if (year < MIN_YEAR || year > maxYear) {
      throw new AcademicValidationException(
          AcademicMessages.ACADEMIC_YEAR_OUT_OF_RANGE,
          Map.of("year", AcademicMessages.ACADEMIC_YEAR_OUT_OF_RANGE));
    }
  }

  private static AcademicValidationException dateValidationError(
      final String field, final String message) {
    return new AcademicValidationException(
        AcademicMessages.ACADEMIC_YEAR_DATES_INVALID, Map.of(field, message));
  }
}
