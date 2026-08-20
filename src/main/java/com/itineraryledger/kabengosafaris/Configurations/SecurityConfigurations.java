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
import com.itineraryledger.kabengosafaris.EndpointPermission.EndpointPermissionService;
import com.itineraryledger.kabengosafaris.Security.CustomUserDetailsService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@lombok.extern.slf4j.Slf4j
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
    public com.itineraryledger.kabengosafaris.Feature.FeatureGateFilter featureGateFilter(
        com.itineraryledger.kabengosafaris.Feature.FeatureService featureService
    ) {
        return new com.itineraryledger.kabengosafaris.Feature.FeatureGateFilter(featureService);
    }

    @Bean
    public DynamicPermissionFilter dynamicPermissionFilter(
        EndpointPermissionService endpointPermissionService
    ) {
        return new DynamicPermissionFilter(endpointPermissionService);
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain staticResourcesFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/images/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }

    @org.springframework.beans.factory.annotation.Value("${app.management.base.url:}")
    private String managementBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:}")
    private String extraAllowedOrigins;

    /**
     * Who may make a credentialed request to this API.
     *
     * The panel's own origin, plus whatever this deployment adds — a second panel hostname during a
     * cutover, or a developer's machine working against a live API, which is a real workflow and
     * better stated than smuggled in behind a wildcard.
     */
    private List<String> allowedOrigins() {
        java.util.LinkedHashSet<String> origins = new java.util.LinkedHashSet<>();

        if (managementBaseUrl != null && !managementBaseUrl.isBlank()) {
            origins.add(managementBaseUrl.trim().replaceAll("/+$", ""));
        }
        if (extraAllowedOrigins != null && !extraAllowedOrigins.isBlank()) {
            for (String origin : extraAllowedOrigins.split(",")) {
                if (!origin.isBlank()) origins.add(origin.trim().replaceAll("/+$", ""));
            }
        }
        if (origins.isEmpty()) {
            /*
             * Never fall back to "everybody". An API that answers nobody is a visible outage somebody
             * fixes in a minute; an API that answers everybody is a hole nobody notices.
             */
            log.error("No CORS origin is configured (app.management.base.url and app.cors.allowed-origins "
                + "are both empty). The panel will be refused by the browser until one is set.");
        }
        log.info("CORS: credentialed requests allowed from {}", origins);
        return List.copyOf(origins);
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        DynamicPermissionFilter dynamicPermissionFilter,
        com.itineraryledger.kabengosafaris.Feature.FeatureGateFilter featureGateFilter
    ) throws Exception {
        return httpSecurity
        // Configure CORS inline using a custom configuration source
        .cors(cors -> {
            CorsConfiguration corsConfiguration = new CorsConfiguration();
            /*
             * Named origins, not a wildcard.
             *
             * This was `*` WITH credentials, which means any page anywhere could make a credentialed
             * request to this API and read the answer — the browser's same-origin protection turned
             * off for every site on the internet at once. It existed for the backup cookie-bridge,
             * which needs credentials; the fix is to keep credentials and name who may use them.
             *
             * The panel's own origin is always allowed, because that is the client this API exists
             * for. Anything else is stated in configuration per deployment.
             *
             * Not affected: the public websites. Their API references are <img> sources, and an image
             * load is not a CORS request.
             */
            corsConfiguration.setAllowedOrigins(allowedOrigins());
            /* OPTIONS for preflight, HEAD because a monitor sends one and a 403 reads as an outage */
            corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
            corsConfiguration.setAllowedHeaders(List.of("*")); // Allow all headers
            corsConfiguration.setExposedHeaders(List.of(
                HttpHeaders.CONTENT_DISPOSITION
            )); // Expose specific headers
            // Required so the browser accepts cross-origin requests carrying
            // cookies (used by the backup download cookie-bridge — a
            // short-lived path-scoped JWT cookie issued by
            // /api/backups/{filename}/prepare-download and read on the
            // following top-level navigation to /api/backups/download/*).
            // Without this, the prepare-download response is rejected with
            // "No response from server" even on 200 OK.
            corsConfiguration.setAllowCredentials(true);

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
                /*
                 * PUBLIC MEDIA — website content, and an <img> carries no bearer token.
                 *
                 * IMAGES only. Every one of these is served to the public site: park and activity
                 * photos, accommodation galleries, heroes, blog art, testimony portraits. A URL is
                 * the only credential, which is exactly right for a picture meant to be published.
                 *
                 * DOCUMENTS are deliberately NOT here. They were, and it meant a customer document —
                 * the module that exists to hold passports and visas — could be fetched by anyone
                 * with the link: no login, no permission check, and links leak through forwarded
                 * mail, browser history and shared screens. The panel reads private files through
                 * the API with its token, so nothing needed the exemption.
                 *
                 * Adding a media module? CompanyMediaExposureTest fails until it is classified.
                 */
                .requestMatchers("/api/accommodation-images/*/file", "/api/accommodation-images/file/*").permitAll()
                .requestMatchers("/api/park-images/*/file", "/api/park-images/file/*").permitAll()
                .requestMatchers("/api/activity-images/*/file", "/api/activity-images/file/*").permitAll()
                .requestMatchers("/api/park-activity-images/*/file", "/api/park-activity-images/file/*").permitAll()
                .requestMatchers("/api/itinerary-images/*/file", "/api/itinerary-images/file/*").permitAll()
                .requestMatchers("/api/hero-images/*/file", "/api/hero-images/file/*").permitAll()
                .requestMatchers("/api/blog-images/*/file", "/api/blog-images/file/*").permitAll()
                /* was missing, so the public site's testimony portraits answered 401 */
                .requestMatchers("/api/testimony-images/*/file", "/api/testimony-images/file/*").permitAll()
                .requestMatchers("/api/heroes/page/*").permitAll() // Allow public access to hero sections by page
                .requestMatchers("/api/public/**").permitAll() // Allow public access to website frontend APIs
                /*
                 * The terse health probe: UP or DOWN, no components, no versions, no paths (see the
                 * `public` health group). Whatever asks — an uptime monitor, a load balancer, the
                 * deploy script — has no token and no way to obtain one, and answering it with a 401
                 * would report the app as broken while it is merely private. The DETAILED health
                 * lives on the localhost-only management port.
                 */
                .requestMatchers("/healthz").permitAll()

                .anyRequest().authenticated() // Require authentication for any other request
        )
        // Enable HTTP Basic authentication for Rest API clients
        .httpBasic(Customizer.withDefaults())
        // Add JWT filter first (before default filters)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        // Add DynamicPermissionFilter before JWT filter for runtime endpoint permission checking
        .addFilterBefore(dynamicPermissionFilter, JwtAuthenticationFilter.class)
        /*
         * The feature gate runs FIRST of the three.
         *
         * A module this company does not have should answer "no such thing" before anybody asks who is
         * calling or what they may do — otherwise a disabled feature's 404 depends on being logged in,
         * and an unauthenticated caller would learn more than a signed-in one.
         */
        .addFilterBefore(featureGateFilter, DynamicPermissionFilter.class)
        .build();
    }
    
}
