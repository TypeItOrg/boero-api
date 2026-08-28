package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_class_schedules")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class CourseClassSchedule extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "course_class_schedule_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_class_day_id", nullable = false)
  private CourseClassDay day;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  public static CourseClassSchedule create(
      final Institution institution,
      final CourseClassDay day,
      final LocalTime startTime,
      final LocalTime endTime) {
    if (!startTime.isBefore(endTime)) {
      throw new AcademicValidationException(
          AcademicMessages.COURSE_SCHEDULE_INVALID,
          Map.of("schedules", AcademicMessages.COURSE_SCHEDULE_INVALID));
    }
    return CourseClassSchedule.builder()
        .institution(institution)
        .day(day)
        .startTime(startTime)
        .endTime(endTime)
        .build();
  }

  public int durationMinutes() {
    return (int) java.time.Duration.between(startTime, endTime).toMinutes();
  }

  public boolean overlaps(final CourseClassSchedule other) {
    return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
  }
}
