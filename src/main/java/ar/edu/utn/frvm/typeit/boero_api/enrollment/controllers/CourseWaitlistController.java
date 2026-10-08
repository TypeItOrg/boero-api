package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresInstitutionAccess;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CourseWaitlistEntryResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListCourseWaitlistUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/institutions/{institutionId}/courses/{courseId}/waitlist")
@RequiresInstitutionAccess
@RequiredArgsConstructor
public class CourseWaitlistController {

  private final ListCourseWaitlistUseCase listCourseWaitlistUseCase;

  @GetMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.COURSE_WAITLIST_READ)
  public List<CourseWaitlistEntryResponse> list(
      @PathVariable final UUID institutionId, @PathVariable final UUID courseId) {
    return listCourseWaitlistUseCase.execute(institutionId, courseId);
  }
}
