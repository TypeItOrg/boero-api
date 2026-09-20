package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import java.util.List;
import java.util.UUID;

public record CourseEnrollmentClassOptionResponse(
    UUID id,
    List<UUID> teacherIds,
    List<CourseEnrollmentTeacherOptionResponse> teachers,
    List<CourseEnrollmentDayOptionResponse> days) {}
