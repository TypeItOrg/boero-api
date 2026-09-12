package ar.edu.utn.frvm.typeit.boero_api.auth.events;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record InstitutionalEmailVerificationRequested(
    UUID userId, String recipientEmail, String institutionName, String fullName, String token) {

  public static InstitutionalEmailVerificationRequested from(final User user, final String token) {
    return new InstitutionalEmailVerificationRequested(
        user.getId(),
        user.getPerson().getEmail(),
        user.getInstitution().getName(),
        user.getName() + " " + user.getLastName(),
        token);
  }
}
