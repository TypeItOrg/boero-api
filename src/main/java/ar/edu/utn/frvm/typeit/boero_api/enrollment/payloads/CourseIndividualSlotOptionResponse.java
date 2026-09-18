package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseIndividualSlot;
import java.time.LocalTime;
import java.util.UUID;

public record CourseIndividualSlotOptionResponse(UUID id, LocalTime startTime, LocalTime endTime) {

  public static CourseIndividualSlotOptionResponse from(final CourseIndividualSlot slot) {
    return new CourseIndividualSlotOptionResponse(
        slot.getId(), slot.getStartTime(), slot.getEndTime());
  }
}
