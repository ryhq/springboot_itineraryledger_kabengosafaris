package com.itineraryledger.kabengosafaris.Health;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The probes answer without a token, because whatever is asking cannot have one.
 *
 * systemd, the deploy script and an uptime monitor have no JWT and no business obtaining one. The
 * detail is not public in exchange: actuator is bound to 127.0.0.1 on its own port (see
 * `management.server.address`), so only the box itself can read the components. What the internet
 * can reach is the terse `public` health group on /api/public/health — a bare UP or DOWN, no
 * component names, no versions, no paths.
 *
 * First in the chain, ahead of the JWT filter, so a probe is never answered with a 401 during the
 * seconds when the database is the thing that is broken.
 */
@Configuration
public class ActuatorSecurityConfiguration {

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }
}
