package edu.hotel.booking.security;

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

                        .requestMatchers(HttpMethod.GET, "/hotels/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/hotels").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/hotels/**").hasAnyRole("ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/hotels/*/activate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/hotels/*/deactivate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/hotels/*/room-types").hasAnyRole("ADMIN", "HOTEL_MANAGER")

                        .requestMatchers(HttpMethod.GET, "/room-types/*/rooms").hasAnyRole("ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/room-types/*/rooms").hasAnyRole("ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/room-types/*/tariffs").hasAnyRole("ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/room-types/*/tariffs").permitAll()

                        .requestMatchers(HttpMethod.PUT, "/rooms/*/status").hasAnyRole("ADMIN", "HOTEL_MANAGER")

                        .requestMatchers(HttpMethod.POST, "/bookings").hasRole("GUEST")
                        .requestMatchers(HttpMethod.GET, "/bookings/**").hasAnyRole("GUEST", "ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/bookings").hasAnyRole("ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/bookings/*/cancel").hasAnyRole("GUEST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/bookings/*/check-in").hasAnyRole("ADMIN", "HOTEL_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/bookings/*/check-out").hasAnyRole("ADMIN", "HOTEL_MANAGER")

                        .requestMatchers(HttpMethod.GET, "/guests/*/bookings").hasAnyRole("GUEST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/guests").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/guests/me").authenticated()

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
