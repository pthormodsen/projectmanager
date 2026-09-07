package no.patreek.projectmanager.config;

import jakarta.servlet.http.HttpServletResponse;
import no.patreek.projectmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        @Value("${app.security.enabled:true}") boolean securityEnabled
    ) throws Exception {
        if (!securityEnabled) {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
            return http.build();
        }

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(
                (request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            ))
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
        UserRepository userRepository
    ) {
        return requestedUsername -> userRepository.findByUsername(requestedUsername)
            .map(user -> User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles("USER")
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
