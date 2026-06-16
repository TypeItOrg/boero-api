package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserResponse(UserPayload user) {

  public static UserResponse from(User user) {
    return UserResponse.builder().user(UserPayload.from(user)).build();
  }

  public static UserResponse from(User user, UUID personId) {
    return UserResponse.builder().user(UserPayload.from(user, personId)).build();
  }
}
