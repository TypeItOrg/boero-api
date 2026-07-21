package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresInstitutionAccess;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.UpdateInstitutionalInstitutionRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.UpdateInstitutionalInstitutionUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/institutions/{institutionId}")
@RequiredArgsConstructor
@RequiresInstitutionAccess
public class InstitutionalInstitutionController {

  private final UpdateInstitutionalInstitutionUseCase updateInstitutionalInstitutionUseCase;

  @PutMapping(version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_UPDATE)
  public InstitutionDetailResponse update(
      @PathVariable final UUID institutionId,
      @Valid @RequestBody final UpdateInstitutionalInstitutionRequest request,
      final Authentication authentication) {
    return updateInstitutionalInstitutionUseCase.execute(institutionId, request);
  }
}
