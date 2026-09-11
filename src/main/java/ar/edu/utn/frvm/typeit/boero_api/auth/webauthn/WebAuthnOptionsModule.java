package ar.edu.utn.frvm.typeit.boero_api.auth.webauthn;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInputs;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInput;
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInputs;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

public class WebAuthnOptionsModule extends SimpleModule {

  public WebAuthnOptionsModule() {
    addDeserializer(PublicKeyCredentialCreationOptions.class, new CreationOptionsDeserializer());
    addDeserializer(PublicKeyCredentialRequestOptions.class, new RequestOptionsDeserializer());
  }

  static final class CreationOptionsDeserializer
      extends ValueDeserializer<PublicKeyCredentialCreationOptions> {

    @Override
    public PublicKeyCredentialCreationOptions deserialize(
        final JsonParser parser, final DeserializationContext context) throws JacksonException {
      final JsonNode root = parser.readValueAsTree();
      requireObject(root);
      final PublicKeyCredentialCreationOptions.PublicKeyCredentialCreationOptionsBuilder builder =
          PublicKeyCredentialCreationOptions.builder();
      builder.rp(rpEntity(required(root, "rp")));
      builder.user(userEntity(required(root, "user")));
      builder.challenge(new Bytes(requiredBytes(root, "challenge")));
      builder.pubKeyCredParams(credParams(required(root, "pubKeyCredParams")));
      builder.timeout(timeout(required(root, "timeout")));
      builder.excludeCredentials(descriptors(required(root, "excludeCredentials")));
      builder.authenticatorSelection(selection(required(root, "authenticatorSelection")));
      builder.attestation(attestation(requiredText(root, "attestation")));
      final AuthenticationExtensionsClientInputs extensions = extensions(root.get("extensions"));
      if (extensions != null) {
        builder.extensions(extensions);
      }
      return builder.build();
    }
  }

  static final class RequestOptionsDeserializer
      extends ValueDeserializer<PublicKeyCredentialRequestOptions> {

    @Override
    public PublicKeyCredentialRequestOptions deserialize(
        final JsonParser parser, final DeserializationContext context) throws JacksonException {
      final JsonNode root = parser.readValueAsTree();
      requireObject(root);
      final PublicKeyCredentialRequestOptions.PublicKeyCredentialRequestOptionsBuilder builder =
          PublicKeyCredentialRequestOptions.builder();
      builder.challenge(new Bytes(requiredBytes(root, "challenge")));
      builder.timeout(timeout(required(root, "timeout")));
      builder.rpId(requiredText(root, "rpId"));
      builder.allowCredentials(descriptors(required(root, "allowCredentials")));
      builder.userVerification(userVerification(requiredText(root, "userVerification")));
      builder.extensions(extensionsOrEmpty(root.get("extensions")));
      return builder.build();
    }
  }

  private static final Map<Long, PublicKeyCredentialParameters> CRED_PARAMS =
      Map.of(
          -7L, PublicKeyCredentialParameters.ES256,
          -35L, PublicKeyCredentialParameters.ES384,
          -36L, PublicKeyCredentialParameters.ES512,
          -257L, PublicKeyCredentialParameters.RS256,
          -258L, PublicKeyCredentialParameters.RS384,
          -259L, PublicKeyCredentialParameters.RS512,
          -8L, PublicKeyCredentialParameters.EdDSA,
          -65535L, PublicKeyCredentialParameters.RS1);

  private static JsonNode required(final JsonNode parent, final String field) {
    final JsonNode child = parent.get(field);
    if (child == null || child.isNull()) {
      throw new IllegalArgumentException("Missing WebAuthn options field");
    }
    return child;
  }

  private static void requireObject(final JsonNode node) {
    if (node == null || !node.isObject()) {
      throw new IllegalArgumentException("Invalid WebAuthn options payload");
    }
  }

  private static String requiredText(final JsonNode parent, final String field) {
    final JsonNode child = required(parent, field);
    if (!child.isString() || child.asString().isEmpty()) {
      throw new IllegalArgumentException("Invalid WebAuthn options field");
    }
    return child.asString();
  }

  private static byte[] requiredBytes(final JsonNode parent, final String field) {
    final byte[] decoded = decodeBase64Url(requiredText(parent, field));
    if (decoded.length == 0) {
      throw new IllegalArgumentException("Invalid WebAuthn options field");
    }
    return decoded;
  }

  private static byte[] decodeBase64Url(final String value) {
    final String padded = value + "=".repeat((4 - (value.length() % 4)) % 4);
    try {
      return Base64.getUrlDecoder().decode(padded);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid WebAuthn options field", exception);
    }
  }

  private static Duration timeout(final JsonNode node) {
    if (!node.isNumber() || node.asLong() < 0) {
      throw new IllegalArgumentException("Invalid WebAuthn options field");
    }
    return Duration.ofMillis(node.asLong());
  }

  private static AuthenticationExtensionsClientInputs extensions(final JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    requireObject(node);
    if (node.size() == 0) {
      return new ImmutableAuthenticationExtensionsClientInputs(List.of());
    }
    final JsonNode credProps = node.get("credProps");
    final boolean onlyCredProps = node.size() == 1 && credProps != null && !credProps.isNull();
    if (!onlyCredProps || !credProps.isBoolean()) {
      throw new IllegalArgumentException("Unsupported WebAuthn options extensions");
    }
    return new ImmutableAuthenticationExtensionsClientInputs(
        List.of(
            new ImmutableAuthenticationExtensionsClientInput<>(
                "credProps", credProps.asBoolean())));
  }

  private static AuthenticationExtensionsClientInputs extensionsOrEmpty(final JsonNode node) {
    final AuthenticationExtensionsClientInputs parsed = extensions(node);
    if (parsed == null) {
      return new ImmutableAuthenticationExtensionsClientInputs(List.of());
    }
    return parsed;
  }

  private static PublicKeyCredentialRpEntity rpEntity(final JsonNode node) {
    requireObject(node);
    return PublicKeyCredentialRpEntity.builder()
        .id(requiredText(node, "id"))
        .name(requiredText(node, "name"))
        .build();
  }

  private static org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
      userEntity(final JsonNode node) {
    requireObject(node);
    return ImmutablePublicKeyCredentialUserEntity.builder()
        .id(new Bytes(requiredBytes(node, "id")))
        .name(requiredText(node, "name"))
        .displayName(requiredText(node, "displayName"))
        .build();
  }

  private static List<PublicKeyCredentialParameters> credParams(final JsonNode node) {
    if (!node.isArray()) {
      throw new IllegalArgumentException("Invalid WebAuthn options field");
    }
    final List<PublicKeyCredentialParameters> params = new ArrayList<>();
    for (final JsonNode entry : node) {
      requireObject(entry);
      if (!"public-key".equals(requiredText(entry, "type"))) {
        throw new IllegalArgumentException("Unsupported WebAuthn credential type");
      }
      final JsonNode alg = required(entry, "alg");
      if (!alg.isNumber()) {
        throw new IllegalArgumentException("Invalid WebAuthn options field");
      }
      params.add(credParam(alg.asLong()));
    }
    return List.copyOf(params);
  }

  private static PublicKeyCredentialParameters credParam(final long alg) {
    final PublicKeyCredentialParameters param = CRED_PARAMS.get(alg);
    if (param == null) {
      throw new IllegalArgumentException("Unsupported WebAuthn credential algorithm");
    }
    return param;
  }

  private static List<PublicKeyCredentialDescriptor> descriptors(final JsonNode node) {
    if (!node.isArray()) {
      throw new IllegalArgumentException("Invalid WebAuthn options field");
    }
    final List<PublicKeyCredentialDescriptor> descriptors = new ArrayList<>();
    for (final JsonNode entry : node) {
      descriptors.add(descriptor(entry));
    }
    return List.copyOf(descriptors);
  }

  private static PublicKeyCredentialDescriptor descriptor(final JsonNode node) {
    requireObject(node);
    if (!"public-key".equals(requiredText(node, "type"))) {
      throw new IllegalArgumentException("Unsupported WebAuthn credential type");
    }
    final PublicKeyCredentialDescriptor.PublicKeyCredentialDescriptorBuilder builder =
        PublicKeyCredentialDescriptor.builder();
    builder.type(PublicKeyCredentialType.PUBLIC_KEY);
    builder.id(new Bytes(requiredBytes(node, "id")));
    final JsonNode transports = node.get("transports");
    if (transports != null && !transports.isNull()) {
      builder.transports(transports(transports));
    }
    return builder.build();
  }

  private static AuthenticatorTransport[] transports(final JsonNode node) {
    if (!node.isArray()) {
      throw new IllegalArgumentException("Invalid WebAuthn options field");
    }
    final List<AuthenticatorTransport> transports = new ArrayList<>();
    for (final JsonNode entry : node) {
      if (!entry.isString()) {
        throw new IllegalArgumentException("Invalid WebAuthn options field");
      }
      transports.add(transport(entry.asString()));
    }
    return transports.toArray(AuthenticatorTransport[]::new);
  }

  private static AuthenticatorTransport transport(final String value) {
    final String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    return switch (normalized) {
      case "USB" -> AuthenticatorTransport.USB;
      case "NFC" -> AuthenticatorTransport.NFC;
      case "BLE" -> AuthenticatorTransport.BLE;
      case "SMART_CARD" -> AuthenticatorTransport.SMART_CARD;
      case "HYBRID" -> AuthenticatorTransport.HYBRID;
      case "INTERNAL" -> AuthenticatorTransport.INTERNAL;
      default -> throw new IllegalArgumentException("Unsupported WebAuthn transport");
    };
  }

  private static AuthenticatorSelectionCriteria selection(final JsonNode node) {
    requireObject(node);
    final AuthenticatorSelectionCriteria.AuthenticatorSelectionCriteriaBuilder builder =
        AuthenticatorSelectionCriteria.builder();
    builder.residentKey(residentKey(requiredText(node, "residentKey")));
    builder.userVerification(userVerification(requiredText(node, "userVerification")));
    final JsonNode attachment = node.get("authenticatorAttachment");
    if (attachment != null && !attachment.isNull()) {
      if (!attachment.isString()) {
        throw new IllegalArgumentException("Invalid WebAuthn options field");
      }
      builder.authenticatorAttachment(authenticatorAttachment(attachment.asString()));
    }
    return builder.build();
  }

  private static ResidentKeyRequirement residentKey(final String value) {
    final String normalized = value.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "required" -> ResidentKeyRequirement.REQUIRED;
      case "preferred" -> ResidentKeyRequirement.PREFERRED;
      case "discouraged" -> ResidentKeyRequirement.DISCOURAGED;
      default -> throw new IllegalArgumentException("Unsupported WebAuthn resident key");
    };
  }

  private static UserVerificationRequirement userVerification(final String value) {
    final String normalized = value.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "required" -> UserVerificationRequirement.REQUIRED;
      case "preferred" -> UserVerificationRequirement.PREFERRED;
      case "discouraged" -> UserVerificationRequirement.DISCOURAGED;
      default -> throw new IllegalArgumentException("Unsupported WebAuthn user verification");
    };
  }

  private static AuthenticatorAttachment authenticatorAttachment(final String value) {
    final String normalized = value.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "platform" -> AuthenticatorAttachment.PLATFORM;
      case "cross-platform" -> AuthenticatorAttachment.CROSS_PLATFORM;
      default ->
          throw new IllegalArgumentException("Unsupported WebAuthn authenticator attachment");
    };
  }

  private static AttestationConveyancePreference attestation(final String value) {
    final String normalized = value.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "none" -> AttestationConveyancePreference.NONE;
      case "indirect" -> AttestationConveyancePreference.INDIRECT;
      case "direct" -> AttestationConveyancePreference.DIRECT;
      case "enterprise" -> AttestationConveyancePreference.ENTERPRISE;
      default -> throw new IllegalArgumentException("Unsupported WebAuthn attestation");
    };
  }
}
