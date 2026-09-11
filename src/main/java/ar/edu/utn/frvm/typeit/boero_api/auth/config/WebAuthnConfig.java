package ar.edu.utn.frvm.typeit.boero_api.auth.config;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.PasskeyUserCredentialRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.PasskeyUserEntityRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.webauthn.WebAuthnOptionsModule;
import java.util.HashSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

@Configuration
public class WebAuthnConfig {

  @Bean
  PublicKeyCredentialRpEntity webauthnRpEntity(final WebAuthnProperties properties) {
    return PublicKeyCredentialRpEntity.builder()
        .id(properties.rpId())
        .name(properties.rpName())
        .build();
  }

  @Bean
  WebAuthnRelyingPartyOperations webauthnRelyingPartyOperations(
      final PasskeyUserEntityRepository userEntityRepository,
      final PasskeyUserCredentialRepository userCredentialRepository,
      final PublicKeyCredentialRpEntity rpEntity,
      final WebAuthnProperties properties) {
    final Webauthn4JRelyingPartyOperations operations =
        new Webauthn4JRelyingPartyOperations(
            userEntityRepository,
            userCredentialRepository,
            rpEntity,
            new HashSet<>(properties.allowedOrigins()));
    operations.setCustomizeCreationOptions(
        builder ->
            builder
                .authenticatorSelection(
                    AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build())
                .attestation(AttestationConveyancePreference.NONE));
    operations.setCustomizeRequestOptions(
        builder -> builder.userVerification(UserVerificationRequirement.REQUIRED));
    return operations;
  }

  @Bean
  WebauthnJacksonModule webauthnJacksonModule() {
    return new WebauthnJacksonModule();
  }

  @Bean
  tools.jackson.databind.ObjectMapper webauthnObjectMapper(
      final WebauthnJacksonModule springModule) {
    return tools.jackson.databind.json.JsonMapper.builder()
        .addModule(springModule)
        .addModule(new WebAuthnOptionsModule())
        .build();
  }
}
