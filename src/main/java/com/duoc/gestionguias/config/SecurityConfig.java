package com.duoc.gestionguias.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /*
     * Claim personalizado que viene desde Azure AD B2C.
     * En el token JWT aparece como extension_guiaRole.
     *
     * Valores esperados:
     * admin    -> puede usar todos los endpoints de guias
     * descarga -> solo puede descargar guias
     */
    @Value("${app.security.claim-role:extension_guiaRole}")
    private String roleClaim;

    /*
     * Configuracion principal de seguridad HTTP.
     * Todas las llamadas deben venir autenticadas con Bearer Token.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            /*
             * Se desactiva CSRF porque el backend funciona como API REST
             * y sera consumido mediante tokens JWT.
             */
            .csrf(csrf -> csrf.disable())

            /*
             * Se permite el uso de la consola H2 en caso de pruebas locales.
             * Si no se usa H2 console, estas reglas no afectan al funcionamiento.
             */
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            .authorizeHttpRequests(auth -> auth

                /*
                 * Permite acceder a la consola H2 sin autenticacion.
                 * Solo aplica si se usa /h2-console en ambiente local.
                 */
                .requestMatchers("/h2-console/**").permitAll()

                /*
                 * Endpoint de descarga:
                 * permitido para usuarios con rol descarga o admin.
                 */
                .requestMatchers("/api/guias/*/descargar")
                    .hasAnyAuthority("ROLE_descarga", "ROLE_admin")

                /*
                 * Resto de endpoints de guias:
                 * permitido solo para usuarios con rol admin.
                 */
                .requestMatchers("/api/guias/**")
                    .hasAuthority("ROLE_admin")

                /*
                 * Cualquier otra ruta requiere autenticacion.
                 */
                .anyRequest()
                    .authenticated()
            )

            /*
             * Configura el backend como Resource Server OAuth2.
             * Esto permite validar JWT emitidos por Azure AD B2C.
             */
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /*
     * Convierte el claim extension_guiaRole en authorities de Spring Security.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthoritiesFromCustomClaim);
        return converter;
    }

    /*
     * Extrae el rol desde el claim personalizado de Azure AD B2C.
     */
    private Collection<GrantedAuthority> extractAuthoritiesFromCustomClaim(Jwt jwt) {
        Object claimValue = jwt.getClaims().get(roleClaim);

        if (claimValue == null) {
            return List.of();
        }

        if (claimValue instanceof String role) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        }

        if (claimValue instanceof Collection<?> roles) {
            return roles.stream()
                    .map(Object::toString)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}