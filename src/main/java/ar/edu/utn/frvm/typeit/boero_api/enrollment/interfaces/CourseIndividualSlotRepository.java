package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseIndividualSlot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseIndividualSlotRepository extends JpaRepository<CourseIndividualSlot, UUID> {

  List<CourseIndividualSlot> findBySchedule_IdOrderByStartTime(UUID scheduleId);

  List<CourseIndividualSlot> findBySchedule_IdIn(List<UUID> scheduleIds);
}
