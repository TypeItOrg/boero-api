package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresInstitutionAccess;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonSummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.TeacherDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.GetInstitutionTeacherDetailUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListInstitutionTeachersUseCase;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/institutions/{institutionId}/teachers")
@RequiredArgsConstructor
@RequiresInstitutionAccess
public class TeacherController {

  private final ListInstitutionTeachersUseCase listInstitutionTeachersUseCase;
  private final GetInstitutionTeacherDetailUseCase getInstitutionTeacherDetailUseCase;

  @GetMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_READ_ANY)
  public PaginatedResponse<PersonSummaryResponse> list(
      @PathVariable final UUID institutionId,
      @RequestParam(required = false) @Size(max = 100) final String search,
      @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC)
          final Pageable pageable) {
    return listInstitutionTeachersUseCase.execute(institutionId, search, pageable);
  }

  @GetMapping(value = "/{teacherId}", version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_READ_ANY)
  public TeacherDetailResponse get(
      @PathVariable final UUID institutionId, @PathVariable final UUID teacherId) {
    return getInstitutionTeacherDetailUseCase.execute(institutionId, teacherId);
  }
}
