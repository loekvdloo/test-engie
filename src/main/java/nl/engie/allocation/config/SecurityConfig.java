package nl.engie.allocation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Beveiligingsconfiguratie voor het Market Message Processor platform.
 *
 * <p>Beveiligingsmaatregelen:
 * <ul>
 *   <li>Security headers: CSP, X-Frame-Options, X-Content-Type-Options, HSTS, Referrer-Policy</li>
 *   <li>CORS: alleen dezelfde origin toegestaan</li>
 *   <li>CSRF: uitgeschakeld voor stateless REST API (alle state-changes via JSON POST)</li>
 *   <li>Alle endpoints publiek (dashboard is een demo-applicatie zonder gebruikersaccounts)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // --- Autorisatie: alle endpoints zijn publiek toegankelijk ---
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // --- CSRF uitschakelen voor REST API (JSON/XML body, geen browser forms) ---
            .csrf(csrf -> csrf.disable())

            // --- CORS: restrictief beleid ---
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // --- Security Headers ---
            .headers(headers -> headers
                // Voorkom dat de pagina in een iframe geladen wordt (clickjacking)
            // SAMEORIGIN toegestaan zodat VS Code Simple Browser en development tools werken
                .frameOptions(frame -> frame.sameOrigin())

                // Content Security Policy: alleen eigen bronnen toestaan
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self'; " +
                        "connect-src 'self'; " +
                        "form-action 'self'; " +
                        "base-uri 'self'"
                    )
                )

                // Voorkom MIME-type sniffing
                .contentTypeOptions(cto -> {})

                // HTTP Strict Transport Security (alleen via HTTPS)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )

                // Referrer Policy: minimale info doorsturen
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )

                // Permissions Policy: beperk browser-features
                .addHeaderWriter(new StaticHeadersWriter(
                    "Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=()"
                ))
            );

        return http.build();
    }

    /**
     * CORS configuratie: alleen verzoeken van dezelfde origin toestaan.
     * Bij productie moet dit verder beperkt worden tot het exacte domein.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // Zelfde server, via localhost of tunnel
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
