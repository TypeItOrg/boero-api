package ar.edu.utn.frvm.typeit.boero_api.security;

import static ar.edu.utn.frvm.typeit.boero_api.security.SecurityConstants.PUBLIC_ROUTES;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final AccessDeniedHandler accessDeniedHandler;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Disable some security features that are not needed
    http.csrf(AbstractHttpConfigurer::disable);
    http.formLogin(AbstractHttpConfigurer::disable);
    http.httpBasic(AbstractHttpConfigurer::disable);
    http.logout(AbstractHttpConfigurer::disable);
    http.rememberMe(AbstractHttpConfigurer::disable);
    http.sessionManagement(AbstractHttpConfigurer::disable);

    // Custom access denied handler and entrypoint
    http.exceptionHandling(
        exception ->
            exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

    // By default, all requests are authenticated
    http.authorizeHttpRequests(
        auth -> auth.requestMatchers(PUBLIC_ROUTES).permitAll().anyRequest().authenticated());

    return http.build();
  }
}
