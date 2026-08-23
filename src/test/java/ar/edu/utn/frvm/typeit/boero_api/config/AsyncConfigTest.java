package ar.edu.utn.frvm.typeit.boero_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

  private ThreadPoolTaskExecutor executor;

  @BeforeEach
  void setUp() {
    executor = new AsyncConfig().mailExecutor();
    executor.initialize();
  }

  @Test
  @DisplayName("mailExecutor usa un pool acotado con prefijo mail-")
  void mailExecutorUsesBoundedPoolWithMailPrefix() {
    assertThat(executor.getCorePoolSize()).isEqualTo(2);
    assertThat(executor.getMaxPoolSize()).isEqualTo(4);
    assertThat(executor.getThreadNamePrefix()).isEqualTo("mail-");
  }

  @Test
  @DisplayName("expone la constante del qualifier del executor de mail")
  void exposesMailExecutorQualifierConstant() {
    assertThat(AsyncConfig.MAIL_EXECUTOR).isEqualTo("mailExecutor");
  }
}
