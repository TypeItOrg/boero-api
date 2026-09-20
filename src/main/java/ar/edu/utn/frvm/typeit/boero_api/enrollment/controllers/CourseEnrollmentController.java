package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresInstitutionAccess;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentHistoryResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseEnrollmentResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CreateManualCourseEnrollmentRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateAcademicEnrollmentStatusRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.WithdrawCourseEnrollmentRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.CourseEnrollmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/institutions/{institutionId}/course-enrollments")
@RequiresInstitutionAccess
@RequiredArgsConstructor
public class CourseEnrollmentController {

  private final CourseEnrollmentService courseEnrollmentService;

  @GetMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.COURSE_ENROLLMENT_READ)
  public PaginatedResponse<CourseEnrollmentResponse> list(
      @PathVariable final UUID institutionId,
      @RequestParam(required = false) final CourseEnrollmentStatus status,
      @RequestParam(required = false) final AcademicEnrollmentStatus academicStatus,
      @PageableDefault(size = 20) final Pageable pageable) {
    return courseEnrollmentService.listInstitutional(institutionId, status, academicStatus, pageable);
  }

  @GetMapping(value = "/mine", version = Version.V1)
  public PaginatedResponse<CourseEnrollmentResponse> listMine(
      @PathVariable final UUID institutionId,
      final Authentication authentication,
      @RequestParam(required = false) final CourseEnrollmentStatus status,
      @RequestParam(required = false) final AcademicEnrollmentStatus academicStatus,
      @PageableDefault(size = 20) final Pageable pageable) {
    final var principal = (JwtAuthenticatedUser) authentication.getPrincipal();
    return courseEnrollmentService.listOwn(
        institutionId, principal.personId(), status, academicStatus, pageable);
  }

  @GetMapping(value = "/{enrollmentId}", version = Version.V1)
  @RequiresPermission(PermissionCode.COURSE_ENROLLMENT_READ)
  public CourseEnrollmentResponse get(
      @PathVariable final UUID institutionId, @PathVariable final UUID enrollmentId) {
    return courseEnrollmentService.get(institutionId, enrollmentId);
  }

  @GetMapping(value = "/{enrollmentId}/history", version = Version.V1)
  @RequiresPermission(PermissionCode.COURSE_ENROLLMENT_READ)
  public List<CourseEnrollmentHistoryResponse> history(
      @PathVariable final UUID institutionId, @PathVariable final UUID enrollmentId) {
    return courseEnrollmentService.history(institutionId, enrollmentId);
  }

  @PostMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.COURSE_ENROLLMENT_CREATE)
  public CourseEnrollmentResponse createManual(
      @PathVariable final UUID institutionId,
      @Valid @RequestBody final CreateManualCourseEnrollmentRequest request,
      final Authentication authentication) {
    return courseEnrollmentService.createManual(
        institutionId, request, currentPersonId(authentication));
  }

  @PostMapping(value = "/{enrollmentId}/withdraw", version = Version.V1)
  @RequiresPermission(PermissionCode.COURSE_ENROLLMENT_WITHDRAW)
  public CourseEnrollmentResponse withdraw(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID enrollmentId,
      @Valid @RequestBody final WithdrawCourseEnrollmentRequest request,
      final Authentication authentication) {
    return courseEnrollmentService.withdraw(
        institutionId, enrollmentId, request, currentPersonId(authentication));
  }

  @PatchMapping(value = "/{enrollmentId}/academic-status", version = Version.V1)
  @RequiresPermission(PermissionCode.COURSE_ENROLLMENT_ACADEMIC_STATUS_UPDATE)
  public CourseEnrollmentResponse updateAcademicStatus(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID enrollmentId,
      @Valid @RequestBody final UpdateAcademicEnrollmentStatusRequest request,
      final Authentication authentication) {
    return courseEnrollmentService.updateAcademicStatus(
        institutionId, enrollmentId, request, currentPersonId(authentication));
  }

  private UUID currentPersonId(final Authentication authentication) {
    return ((JwtAuthenticatedUser) authentication.getPrincipal()).personId();
  }
}
