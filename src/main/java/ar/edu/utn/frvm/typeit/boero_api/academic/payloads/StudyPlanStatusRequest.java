package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record StudyPlanStatusRequest(@NotNull StudyPlanStatus status, LocalDate effectiveTo) {}
