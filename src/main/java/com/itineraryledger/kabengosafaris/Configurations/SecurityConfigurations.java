package com.itineraryledger.kabengosafaris.Configurations;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.itineraryledger.kabengosafaris.Security.JwtAuthenticationFilter;
import com.itineraryledger.kabengosafaris.Security.DynamicPermissionFilter;
import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.CustomUserDetailsService;
import com.itineraryledger.kabengosafaris.Permission.EndpointPermissionService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration // Marks this class as a configuration class
@EnableWebSecurity // For enabling Spring Security's web security support
@EnableMethodSecurity(prePostEnabled = true) // To enable method-level security like @PreAuthorize
public class SecurityConfigurations {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider,
        CustomUserDetailsService customUserDetailsService
    ) {
        return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
    }

    @Bean
    public DynamicPermissionFilter dynamicPermissionFilter(
        EndpointPermissionService endpointPermissionService
    ) {
        return new DynamicPermissionFilter(endpointPermissionService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        DynamicPermissionFilter dynamicPermissionFilter
    ) throws Exception {
        return httpSecurity
        // Configure CORS inline using a custom configuration source
        .cors(cors -> {
            CorsConfiguration corsConfiguration = new CorsConfiguration();
            corsConfiguration.setAllowedOrigins(List.of("*")); // Allow all origins
            corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH")); // Allow specific methods
            corsConfiguration.setAllowedHeaders(List.of("*")); // Allow all headers
            corsConfiguration.setExposedHeaders(List.of(
                HttpHeaders.CONTENT_DISPOSITION
            )); // Expose specific headers

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); // Create a configuration source
            source.registerCorsConfiguration("/**", corsConfiguration); // Apply CORS configuration to all endpoints
            cors.configurationSource(source);  // Set the configuration source
        })

        .csrf(csrf -> csrf.disable()) // Disable CSRF as we're using a stateless REST API
        .exceptionHandling(
            // Custom handling for unauthorized access
            exception -> exception.authenticationEntryPoint(
                (request, response, authException) -> {
                    response.sendError(401, "Unauthorized");
                }
            )
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Set session management to stateless because we're using JWT
        .authorizeHttpRequests(authorizeHttpRequest -> authorizeHttpRequest
                .requestMatchers("/api/auth/**").permitAll() // Allow unauthenticated access to auth endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Allow unauthenticated access to Swagger documentation
                .anyRequest().authenticated() // Require authentication for any other request
        )
        // Enable HTTP Basic authentication for Rest API clients
        .httpBasic(Customizer.withDefaults())
        // Add JWT filter first (before default filters)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        // Add DynamicPermissionFilter before JWT filter for runtime endpoint permission checking
        .addFilterBefore(dynamicPermissionFilter, JwtAuthenticationFilter.class)
        .build();
    }
    
}
