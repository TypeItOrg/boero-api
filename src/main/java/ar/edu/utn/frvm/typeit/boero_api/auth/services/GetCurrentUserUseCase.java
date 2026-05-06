package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.UserDisabledException;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public UserResponse execute(JwtAuthenticatedUser principal) {
    var user =
        userRepository.findById(principal.userId()).orElseThrow(InvalidCredentialsException::new);
    if (!user.isEnabled()) {
      throw new UserDisabledException();
    }
    return UserResponse.from(user);
  }
}
