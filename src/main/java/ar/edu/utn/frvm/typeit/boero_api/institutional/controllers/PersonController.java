package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.UpdatePersonRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.GetPersonUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.UpdatePersonUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/person")
@RequiredArgsConstructor
public class PersonController {

  private final GetPersonUseCase getPersonUseCase;
  private final UpdatePersonUseCase updatePersonUseCase;

  @GetMapping(value = "/me", version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_READ_OWN)
  public PersonResponse me(Authentication authentication) {
    JwtAuthenticatedUser principal = (JwtAuthenticatedUser) authentication.getPrincipal();
    return getPersonUseCase.execute(principal);
  }

  @PutMapping(value = "/me", version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_PERSON_UPDATE_OWN)
  public PersonResponse updateMe(
      Authentication authentication, @Valid @RequestBody UpdatePersonRequest request) {
    JwtAuthenticatedUser principal = (JwtAuthenticatedUser) authentication.getPrincipal();
    return updatePersonUseCase.execute(principal, request);
  }
}
