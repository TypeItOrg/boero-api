package ar.edu.utn.frvm.typeit.boero_api.auth.webauthn;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Service
public class WebAuthnOptionsCodec {

  private static final String ENCODE_FAILED = "WebAuthn options could not be serialized.";
  private static final String SNAPSHOT_UNREADABLE = "WebAuthn options snapshot unreadable.";
  private static final String REBUILD_UNREADABLE = "WebAuthn options snapshot rebuild unreadable.";
  private static final String CREDENTIAL_UNREADABLE = "WebAuthn credential unreadable.";

  private final ObjectMapper objectMapper;

  public WebAuthnOptionsCodec(@Qualifier("webauthnObjectMapper") final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String encodeCreationOptions(final PublicKeyCredentialCreationOptions options) {
    try {
      return objectMapper.writeValueAsString(options);
    } catch (JacksonException exception) {
      throw new IllegalStateException(ENCODE_FAILED, exception);
    }
  }

  public JsonNode creationOptionsTree(final PublicKeyCredentialCreationOptions options) {
    final ObjectNode root = JsonNodeFactory.instance.objectNode();
    root.put("challenge", base64Url(options.getChallenge()));
    root.set("rp", rpNode(options.getRp()));
    root.set("user", userNode(options.getUser()));
    root.set("pubKeyCredParams", credParamsNode(options.getPubKeyCredParams()));
    root.put("timeout", options.getTimeout().toMillis());
    root.set("excludeCredentials", descriptorsNode(options.getExcludeCredentials()));
    root.set("authenticatorSelection", selectionNode(options.getAuthenticatorSelection()));
    root.put("attestation", options.getAttestation().getValue());
    return root;
  }

  public PublicKeyCredentialCreationOptions decodeCreationOptions(final String json) {
    return rebuildCreationOptions(parseSnapshot(json));
  }

  public JsonNode parseSnapshot(final String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JacksonException | IllegalArgumentException exception) {
      throw new IllegalStateException(SNAPSHOT_UNREADABLE, exception);
    }
  }

  public PublicKeyCredentialCreationOptions rebuildCreationOptions(final JsonNode snapshot) {
    try {
      return objectMapper.treeToValue(snapshot, PublicKeyCredentialCreationOptions.class);
    } catch (JacksonException | IllegalArgumentException exception) {
      throw new IllegalStateException(REBUILD_UNREADABLE, exception);
    }
  }

  public String encodeRequestOptions(final PublicKeyCredentialRequestOptions options) {
    try {
      return objectMapper.writeValueAsString(options);
    } catch (JacksonException exception) {
      throw new IllegalStateException(ENCODE_FAILED, exception);
    }
  }

  public JsonNode requestOptionsTree(final PublicKeyCredentialRequestOptions options) {
    final ObjectNode root = JsonNodeFactory.instance.objectNode();
    root.put("challenge", base64Url(options.getChallenge()));
    root.put("rpId", options.getRpId());
    root.put("timeout", options.getTimeout().toMillis());
    root.set("allowCredentials", descriptorsNode(options.getAllowCredentials()));
    root.put("userVerification", options.getUserVerification().getValue());
    return root;
  }

  private static String base64Url(final Bytes value) {
    return value.toBase64UrlString();
  }

  private static ObjectNode rpNode(final PublicKeyCredentialRpEntity rp) {
    final ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("id", rp.getId());
    node.put("name", rp.getName());
    return node;
  }

  private static ObjectNode userNode(final PublicKeyCredentialUserEntity user) {
    final ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("id", base64Url(user.getId()));
    node.put("name", user.getName());
    node.put("displayName", user.getDisplayName());
    return node;
  }

  private static ArrayNode credParamsNode(final List<PublicKeyCredentialParameters> params) {
    final ArrayNode array = JsonNodeFactory.instance.arrayNode();
    if (params != null) {
      for (final PublicKeyCredentialParameters param : params) {
        final ObjectNode entry = JsonNodeFactory.instance.objectNode();
        entry.put("type", param.getType().getValue());
        entry.put("alg", param.getAlg().getValue());
        array.add(entry);
      }
    }
    return array;
  }

  private static ArrayNode descriptorsNode(final List<PublicKeyCredentialDescriptor> descriptors) {
    final ArrayNode array = JsonNodeFactory.instance.arrayNode();
    if (descriptors != null) {
      for (final PublicKeyCredentialDescriptor descriptor : descriptors) {
        array.add(descriptorNode(descriptor));
      }
    }
    return array;
  }

  private static ObjectNode descriptorNode(final PublicKeyCredentialDescriptor descriptor) {
    final ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("id", base64Url(descriptor.getId()));
    node.put("type", descriptor.getType().getValue());
    if (descriptor.getTransports() != null) {
      final ArrayNode transports = JsonNodeFactory.instance.arrayNode();
      for (final AuthenticatorTransport transport : descriptor.getTransports()) {
        transports.add(transport.getValue());
      }
      node.set("transports", transports);
    }
    return node;
  }

  private static ObjectNode selectionNode(final AuthenticatorSelectionCriteria selection) {
    final ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("residentKey", selection.getResidentKey().getValue());
    node.put("userVerification", selection.getUserVerification().getValue());
    final AuthenticatorAttachment attachment = selection.getAuthenticatorAttachment();
    if (attachment != null) {
      node.put("authenticatorAttachment", attachment.getValue());
    }
    return node;
  }

  public PublicKeyCredentialRequestOptions decodeRequestOptions(final String json) {
    return rebuildRequestOptions(parseSnapshot(json));
  }

  public PublicKeyCredentialRequestOptions rebuildRequestOptions(final JsonNode snapshot) {
    try {
      return objectMapper.treeToValue(snapshot, PublicKeyCredentialRequestOptions.class);
    } catch (JacksonException | IllegalArgumentException exception) {
      throw new IllegalStateException(REBUILD_UNREADABLE, exception);
    }
  }

  public PublicKeyCredential<AuthenticatorAttestationResponse> decodeAttestationCredential(
      final JsonNode node) {
    try {
      return objectMapper.convertValue(
          node, new TypeReference<PublicKeyCredential<AuthenticatorAttestationResponse>>() {});
    } catch (JacksonException | IllegalArgumentException exception) {
      throw new IllegalStateException(CREDENTIAL_UNREADABLE, exception);
    }
  }

  public PublicKeyCredential<AuthenticatorAssertionResponse> decodeAssertionCredential(
      final JsonNode node) {
    try {
      return objectMapper.convertValue(
          node, new TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {});
    } catch (JacksonException | IllegalArgumentException exception) {
      throw new IllegalStateException(CREDENTIAL_UNREADABLE, exception);
    }
  }
}
