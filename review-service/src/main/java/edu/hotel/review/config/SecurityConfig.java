package edu.hotel.review.config;

import edu.hotel.review.security.JwtAuthentificationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthentificationFilter jwtAuthentificationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, "/reviews").hasRole("GUEST")
                        .requestMatchers(HttpMethod.GET, "/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/hotels/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.GET, "/room-types/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.GET, "/hotels/*/rating").authenticated()
                        .requestMatchers(HttpMethod.POST, "/reviews/*/response").hasAnyRole("ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/reviews/*").hasRole("ADMIN")



                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthentificationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("UserDetailsService not used");
        };
    }
}
