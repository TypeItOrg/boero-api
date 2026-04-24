package ar.edu.utn.frvm.typeit.boero_api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Disable some security features that are not needed
    http.csrf(AbstractHttpConfigurer::disable);
    http.formLogin(AbstractHttpConfigurer::disable);
    http.httpBasic(AbstractHttpConfigurer::disable);
    http.logout(AbstractHttpConfigurer::disable);
    http.rememberMe(AbstractHttpConfigurer::disable);
    http.sessionManagement(AbstractHttpConfigurer::disable);

    // By default, all requests are authenticated
    http.authorizeHttpRequests(
        auth -> auth.requestMatchers("/health-check").permitAll().anyRequest().authenticated());

    return http.build();
  }
}
