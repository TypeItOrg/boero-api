package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassDay;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassSchedule;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseDay;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseIndividualSlot;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.CourseIndividualSlotRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseIndividualSlotFactoryTest {
  @Mock CourseIndividualSlotRepository repository;
  @Captor ArgumentCaptor<List<CourseIndividualSlot>> slots;

  @ParameterizedTest
  @CsvSource({"10:00,11:00,30,2", "23:00,23:30,30,1", "10:00:30,11:00:30,30,2"})
  void createsOnlyCompletePeriodsInsideSchedule(String start, String end, int minutes, int count) {
    final var day = CourseClassDay.create(null, null, CourseDay.MONDAY, count, minutes);
    final var schedule =
        CourseClassSchedule.create(null, day, LocalTime.parse(start), LocalTime.parse(end));

    new CourseIndividualSlotFactory(repository).createFor(schedule);

    verify(repository).saveAll(slots.capture());
    assertThat(slots.getValue())
        .hasSize(count)
        .allSatisfy(
            slot -> {
              assertThat(slot.getStartTime()).isAfterOrEqualTo(schedule.getStartTime());
              assertThat(slot.getEndTime()).isBeforeOrEqualTo(schedule.getEndTime());
              assertThat(slot.getStartTime()).isBefore(slot.getEndTime());
            });
    assertThat(slots.getValue().getLast().getEndTime()).isEqualTo(schedule.getEndTime());
  }

  @ParameterizedTest
  @CsvSource({"23:00,23:30:30", "10:00,11:00:30", "10:00,11:00:00.000000001", "10:00,10:15"})
  void rejectsIncompletePeriodsWithoutTruncatingSeconds(String start, String end) {
    final var day = CourseClassDay.create(null, null, CourseDay.MONDAY, 2, 30);

    assertThatThrownBy(
            () ->
                CourseClassSchedule.create(null, day, LocalTime.parse(start), LocalTime.parse(end)))
        .isInstanceOf(AcademicValidationException.class)
        .hasMessage(AcademicMessages.COURSE_PERIOD_DURATION_NOT_DIVISIBLE);
    verifyNoInteractions(repository);
  }

  @Test
  void doesNotCreatePeriodsForGroupClasses() {
    final var day = CourseClassDay.create(null, null, CourseDay.MONDAY, 20, null);
    final var schedule =
        CourseClassSchedule.create(null, day, LocalTime.of(10, 0), LocalTime.of(11, 0));

    new CourseIndividualSlotFactory(repository).createFor(schedule);

    verifyNoInteractions(repository);
  }
}
