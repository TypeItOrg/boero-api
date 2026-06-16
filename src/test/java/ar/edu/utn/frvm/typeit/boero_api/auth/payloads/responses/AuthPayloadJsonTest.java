package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class AuthPayloadJsonTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PERSON_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Autowired private JacksonTester<AuthResponse> authResponseJson;
  @Autowired private JacksonTester<UserResponse> userResponseJson;
  @Autowired private JacksonTester<UserRegisteredResponse> userRegisteredResponseJson;
  @Autowired private JacksonTester<TokenResponse> tokenResponseJson;

  @Test
  @DisplayName("Should serialize auth response with user and tokens")
  void shouldSerializeAuthResponseWithUserAndTokens() throws IOException {
    AuthResponse response =
        AuthResponse.builder()
            .user(userPayload())
            .tokens(
                TokenResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .build())
            .build();

    var json = authResponseJson.write(response);

    assertThat(json).extractingJsonPathStringValue("$.user.userId").isEqualTo(USER_ID.toString());
    assertThat(json)
        .extractingJsonPathStringValue("$.user.personId")
        .isEqualTo(PERSON_ID.toString());
    assertThat(json).extractingJsonPathStringValue("$.user.name").isEqualTo("Ana");
    assertThat(json).extractingJsonPathStringValue("$.user.lastName").isEqualTo("Garcia");
    assertThat(json).extractingJsonPathStringValue("$.user.documentNumber").isEqualTo("12345678");
    assertThat(json)
        .extractingJsonPathStringValue("$.user.institutionId")
        .isEqualTo(INSTITUTION_ID.toString());
    assertThat(json)
        .extractingJsonPathStringValue("$.tokens.accessToken")
        .isEqualTo("access-token");
    assertThat(json)
        .extractingJsonPathStringValue("$.tokens.refreshToken")
        .isEqualTo("refresh-token");
  }

  @Test
  @DisplayName("Should serialize user response as user wrapper")
  void shouldSerializeUserResponseAsUserWrapper() throws IOException {
    UserResponse response = UserResponse.builder().user(userPayload()).build();

    var json = userResponseJson.write(response);

    assertThat(json).extractingJsonPathStringValue("$.user.userId").isEqualTo(USER_ID.toString());
    assertThat(json).doesNotHaveJsonPath("$.userId");
  }

  @Test
  @DisplayName("Should serialize registered user id as userId")
  void shouldSerializeRegisteredUserIdAsUserId() throws IOException {
    UserRegisteredResponse response =
        UserRegisteredResponse.builder()
            .userId(USER_ID)
            .documentNumber("12345678")
            .institutionId(INSTITUTION_ID)
            .build();

    var json = userRegisteredResponseJson.write(response);

    assertThat(json).extractingJsonPathStringValue("$.userId").isEqualTo(USER_ID.toString());
    assertThat(json).doesNotHaveJsonPath("$.id");
  }

  @Test
  @DisplayName("Should serialize token response contract")
  void shouldSerializeTokenResponseContract() throws IOException {
    TokenResponse response =
        TokenResponse.builder().accessToken("access-token").refreshToken("refresh-token").build();

    var json = tokenResponseJson.write(response);

    assertThat(json).extractingJsonPathStringValue("$.accessToken").isEqualTo("access-token");
    assertThat(json).extractingJsonPathStringValue("$.refreshToken").isEqualTo("refresh-token");
  }

  private static UserPayload userPayload() {
    return UserPayload.builder()
        .userId(USER_ID)
        .personId(PERSON_ID)
        .name("Ana")
        .lastName("Garcia")
        .documentNumber("12345678")
        .institutionId(INSTITUTION_ID)
        .build();
  }
}
