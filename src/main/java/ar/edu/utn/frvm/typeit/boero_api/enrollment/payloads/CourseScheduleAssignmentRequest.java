package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseScheduleAssignmentRequest(
    @NotNull UUID classScheduleId, UUID individualSlotId) {}
