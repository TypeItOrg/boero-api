package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefreshRotationPolicyTest {

  @Test
  void activeTokenRotates() {
    assertThat(RefreshRotationPolicy.decide(false, false, Optional.empty()))
        .isInstanceOf(RefreshRotationDecision.Rotate.class);
  }

  @Test
  void revokedTokenReplaysWhenReplayIsAvailable() {
    final RefreshReplay replay = new RefreshReplay("access", "refresh");

    assertThat(RefreshRotationPolicy.decide(true, false, Optional.of(replay)))
        .isEqualTo(new RefreshRotationDecision.Replay(replay));
  }

  @Test
  void revokedTokenIsRejectedAsReuseWhenReplayIsMissing() {
    assertThat(RefreshRotationPolicy.decide(true, false, Optional.empty()))
        .isInstanceOf(RefreshRotationDecision.Reuse.class);
  }

  @Test
  void expiredTokenIsRejected() {
    assertThat(RefreshRotationPolicy.decide(false, true, Optional.empty()))
        .isInstanceOf(RefreshRotationDecision.Invalid.class);
  }
}
