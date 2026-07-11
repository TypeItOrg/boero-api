package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresInstitutionAccess;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InitialRoleAssignmentGuard;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.CreatePersonRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonSummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.UpdatePersonByAdminRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.CreatePersonUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.DeletePersonUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.GetPersonByIdUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListPeopleUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.UpdatePersonByAdminUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/institutions/{institutionId}/people")
@RequiredArgsConstructor
@RequiresInstitutionAccess
public class PeopleController {

  private final ListPeopleUseCase listPeopleUseCase;
  private final GetPersonByIdUseCase getPersonByIdUseCase;
  private final CreatePersonUseCase createPersonUseCase;
  private final UpdatePersonByAdminUseCase updatePersonByAdminUseCase;
  private final DeletePersonUseCase deletePersonUseCase;
  private final InitialRoleAssignmentGuard initialRoleAssignmentGuard;

  @GetMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_READ_ANY)
  public PaginatedResponse<PersonSummaryResponse> list(
      @PathVariable final UUID institutionId,
      @RequestParam(required = false) @Size(max = 100) final String search,
      @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC)
          final Pageable pageable,
      final Authentication authentication) {
    return listPeopleUseCase.execute(institutionId, search, pageable);
  }

  @GetMapping(value = "/{personId}", version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_READ_ANY)
  public PersonResponse get(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID personId,
      final Authentication authentication) {
    return getPersonByIdUseCase.execute(institutionId, personId);
  }

  @PostMapping(version = Version.V1)
  @ResponseStatus(HttpStatus.CREATED)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_CREATE)
  public PersonResponse create(
      @PathVariable final UUID institutionId,
      @Valid @RequestBody final CreatePersonRequest request,
      final Authentication authentication) {
    initialRoleAssignmentGuard.check(authentication, request.initialRole());
    return createPersonUseCase.execute(institutionId, request);
  }

  @PutMapping(value = "/{personId}", version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_UPDATE_ANY)
  public PersonResponse update(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID personId,
      @Valid @RequestBody final UpdatePersonByAdminRequest request,
      final Authentication authentication) {
    return updatePersonByAdminUseCase.execute(institutionId, personId, request);
  }

  @DeleteMapping(value = "/{personId}", version = Version.V1)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_DELETE)
  public void delete(
      @PathVariable final UUID institutionId,
      @PathVariable final UUID personId,
      final Authentication authentication) {
    deletePersonUseCase.execute(institutionId, personId);
  }
}
