package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPlatformRole;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionListItemResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.CreateInstitutionRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.UpdateInstitutionRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.CreateInstitutionUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.GetInstitutionUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListInstitutionsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.UpdateInstitutionUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/institutions")
@RequiredArgsConstructor
public class InstitutionController {

  private final ListInstitutionsUseCase listInstitutionsUseCase;
  private final GetInstitutionUseCase getInstitutionUseCase;
  private final CreateInstitutionUseCase createInstitutionUseCase;
  private final UpdateInstitutionUseCase updateInstitutionUseCase;

  @GetMapping(version = Version.V1)
  public PaginatedResponse<InstitutionListItemResponse> list(
      @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
    return listInstitutionsUseCase.execute(pageable);
  }

  @GetMapping(value = "/{id}", version = Version.V1)
  public InstitutionDetailResponse get(@PathVariable UUID id) {
    return getInstitutionUseCase.execute(id);
  }

  @PostMapping(version = Version.V1)
  @ResponseStatus(HttpStatus.CREATED)
  @RequiresPlatformRole(PlatformRoleCode.PLATFORM_ADMIN)
  public InstitutionDetailResponse create(@Valid @RequestBody CreateInstitutionRequest request) {
    return createInstitutionUseCase.execute(request);
  }

  @PutMapping(value = "/{id}", version = Version.V1)
  @RequiresPlatformRole(PlatformRoleCode.PLATFORM_ADMIN)
  public InstitutionDetailResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateInstitutionRequest request) {
    return updateInstitutionUseCase.execute(id, request);
  }
}
