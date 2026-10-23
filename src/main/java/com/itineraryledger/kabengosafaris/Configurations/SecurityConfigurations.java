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
    public com.itineraryledger.kabengosafaris.WebsiteCache.WebsiteCacheFilter websiteCacheFilter(
        com.itineraryledger.kabengosafaris.WebsiteCache.WebsiteCacheService cacheService,
        com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository profileRepository
    ) {
        return new com.itineraryledger.kabengosafaris.WebsiteCache.WebsiteCacheFilter(
            cacheService, profileRepository);
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

    /**
     * The client portal: a different kind of caller, so a different chain.
     *
     * <p>Everything else here authenticates a member of staff holding a JWT. A client holds no
     * account and no password, and their session is a stored secret in a cookie, because this
     * application mints a fresh JWT signing key on every restart by design and a client's trip is
     * nine months away.
     *
     * <p>So this chain deliberately carries <strong>neither</strong> the JWT filter nor the
     * dynamic permission filter. A portal request has no {@code User} principal and no
     * {@code PERM_} authority to check, and running those two over it would mean a non-staff
     * caller is evaluated by staff rules, which is how somebody eventually gets an answer they
     * should not have.
     *
     * <p>Authorisation instead happens in one place, {@code PortalAccessService.resolve}, which
     * every portal endpoint asks the same question: what may the holder of this link and this
     * cookie see. The chain itself permits the requests and refuses nothing, which is honest
     * about where the decision is made rather than spreading it over two mechanisms.
     *
     * <p>Two filters it does carry. The <strong>feature gate</strong>, so a company without the
     * portal answers 404 rather than serving it; it could not be reached under
     * {@code /api/public/}, which the gate skips, and that is why the portal has its own path
     * space. And the <strong>rate limiter</strong>, because asking for a verification code is
     * unauthenticated work that sends an email.
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain clientPortalFilterChain(
        HttpSecurity http,
        com.itineraryledger.kabengosafaris.Feature.FeatureGateFilter featureGateFilter,
        com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimitFilter rateLimitFilter
    ) throws Exception {
        return http
            .securityMatcher("/portal/**", "/api/portal/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            /*
             * The portal is served from its own hostname, so the browser asks before it calls.
             *
             * Credentials are deliberately OFF. The session is a token the page sends in a
             * header, not a cookie, because the panel's hostname and this API's are different
             * registrable domains for at least one company and a SameSite cookie would simply
             * never arrive. A header the browser does not attach by itself is also why nothing
             * here needs a CSRF token: a page on another origin can make the request, but it
             * cannot make the browser add the one header that matters.
             */
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration portal = new CorsConfiguration();
                portal.setAllowedOrigins(portalOrigins());
                portal.setAllowedMethods(List.of("GET", "POST", "HEAD", "OPTIONS"));
                portal.setAllowedHeaders(List.of("Content-Type", "Accept", PORTAL_SESSION_HEADER));
                portal.setAllowCredentials(false);
                portal.setMaxAge(1800L);
                return portal;
            }))
            /*
             * CSRF off here matches the rest of this API, and the portal does not rely on it:
             * the session cookie is SameSite=Lax, so a form posted from another origin does not
             * carry it, and every state change is a POST.
             */
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                /*
                 * The portal renders no client-supplied markup and loads nothing from anywhere
                 * else. Saying so costs one header and removes the whole class of injected
                 * script, which matters more here than anywhere else in the application: these
                 * pages carry somebody's personal details and are opened on machines we know
                 * nothing about.
                 */
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self'; object-src 'none'; base-uri 'none'; "
                    + "form-action 'self'; frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                        .ReferrerPolicy.SAME_ORIGIN)))
            .addFilterBefore(rateLimitFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(featureGateFilter,
                com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimitFilter.class)
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

    /** The one header a portal page sends to say who it is. Not a cookie; see the chain above. */
    public static final String PORTAL_SESSION_HEADER = "X-Portal-Session";

    @org.springframework.beans.factory.annotation.Value("${portal.base.url:}")
    private String portalBaseUrl;

    /**
     * Where the client portal is served from.
     *
     * <p>Usually my.&lt;domain&gt;, carrying the same build as the panel. The management origin
     * is included too, so the pages work during a cutover and so a developer running the panel
     * locally against a live API is not mysteriously refused.
     */
    private List<String> portalOrigins() {
        java.util.LinkedHashSet<String> origins = new java.util.LinkedHashSet<>();
        if (portalBaseUrl != null && !portalBaseUrl.isBlank()) {
            origins.add(portalBaseUrl.trim().replaceAll("/+$", ""));
        }
        origins.addAll(allowedOrigins());
        return List.copyOf(origins);
    }

    @Bean
    @org.springframework.core.annotation.Order(3)
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        DynamicPermissionFilter dynamicPermissionFilter,
        com.itineraryledger.kabengosafaris.Feature.FeatureGateFilter featureGateFilter,
        com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimitFilter rateLimitFilter,
        com.itineraryledger.kabengosafaris.WebsiteCache.WebsiteCacheFilter websiteCacheFilter
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
             * The public websites are handled SEPARATELY, below. An earlier version of this comment
             * claimed they were unaffected because "their API references are <img> sources, and an
             * image load is not a CORS request". Only the pictures are. The sites also FETCH their
             * parks, heroes, accommodations and testimonies from /api/public/*, and locking this to
             * the panel's origin emptied both of them — the API kept answering 200 with every row
             * present, and the browser threw the answers away.
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

            /*
             * The public API answers any origin, and carries no credentials while doing it.
             *
             * These endpoints are published by definition: a park's description, a hero image, the
             * testimonials — served to a website whose whole job is showing them to strangers, and
             * readable by anyone who types the URL. CORS protects a browser from being used as
             * somebody's authenticated proxy, and with credentials off there is no authority to
             * borrow, so naming origins here would buy nothing and cost an outage every time a
             * site gains a www, a staging host or a preview deployment.
             *
             * allowCredentials(false) is the load-bearing half. A wildcard WITH credentials is the
             * hole this whole configuration exists to close, and the browser refuses that pairing
             * anyway — which is a good sign it should not be attempted.
             *
             * POST is included because the sites submit to it: a testimonial, a newsletter
             * subscription, a booking inquiry.
             */
            CorsConfiguration publicConfiguration = new CorsConfiguration();
            publicConfiguration.setAllowedOriginPatterns(List.of("*"));
            publicConfiguration.setAllowedMethods(List.of("GET", "POST", "HEAD", "OPTIONS"));
            publicConfiguration.setAllowedHeaders(List.of("*"));
            publicConfiguration.setAllowCredentials(false);

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            /* the more specific pattern is registered first — the source takes the first match */
            source.registerCorsConfiguration("/api/public/**", publicConfiguration);
            source.registerCorsConfiguration("/**", corsConfiguration);
            cors.configurationSource(source);
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
        /*
         * The rate limiter runs before all of them.
         *
         * Everything it guards is reachable without an account, so making it wait behind
         * authentication would mean the work it exists to prevent — a database lookup and an outgoing
         * email — happens on the way to being refused. It sits after CORS, though, which Spring puts
         * at the head of the chain: a 429 the browser cannot read is a form that silently does
         * nothing.
         */
        .addFilterBefore(rateLimitFilter, com.itineraryledger.kabengosafaris.Feature.FeatureGateFilter.class)
        /*
         * The website cache trigger is OUTERMOST of the three, because it is the only one that does
         * its work on the way back out. It has to see the status the client will actually get: a
         * save refused by the permission filter, the feature gate or the rate limiter changed
         * nothing, and clearing a website because of it would be a lie told at some expense.
         */
        .addFilterBefore(websiteCacheFilter, com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimitFilter.class)
        .build();
    }
    
}
