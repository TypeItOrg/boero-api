package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseDay;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_class_days")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class CourseClassDay extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "course_class_day_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_class_id", nullable = false)
  private CourseClass courseClass;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 20)
  private CourseDay dayOfWeek;

  @Column(nullable = true)
  private Integer capacity;

  @Column(name = "period_duration_minutes", nullable = true)
  private Integer periodDurationMinutes;

  public static CourseClassDay create(
      final Institution institution,
      final CourseClass courseClass,
      final CourseDay dayOfWeek,
      final Integer capacity,
      final Integer periodDurationMinutes) {
    return CourseClassDay.builder()
        .institution(institution)
        .courseClass(courseClass)
        .dayOfWeek(dayOfWeek)
        .capacity(capacity)
        .periodDurationMinutes(periodDurationMinutes)
        .build();
  }
}
