package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseSelectionDto(@NotNull UUID courseId, UUID preferredTeacherId) {}
