package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

  private final UserRepository userRepository;

  public UserResponse execute(JwtAuthenticatedUser principal) {
    var user =
        userRepository
            .findWithPersonAndInstitutionById(principal.userId())
            .orElseThrow(InvalidCredentialsException::new);
    if (!user.isEnabled()) {
      throw new DisabledException(AuthMessages.USER_DISABLED);
    }
    return UserResponse.from(user, principal.personId());
  }
}
