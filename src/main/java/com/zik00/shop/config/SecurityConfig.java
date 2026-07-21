package com.zik00.shop.config;

import com.zik00.admin.config.AdminSessionAuthenticationFilter;
import com.zik00.shop.service.auth.OAuthUserService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.JwtService;
import com.zik00.shop.service.auth.RedisRefreshTokenStore;
import com.zik00.shop.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final OAuthUserService oauthUserService;
    private final OAuthLoginSuccessHandler oauthLoginSuccessHandler;
    private final RegistrationService registrationService;
    private final WebClientOrigins webClientOrigins;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RedisRefreshTokenStore refreshTokenStore;

    public SecurityConfig(
            OAuthUserService oauthUserService,
            OAuthLoginSuccessHandler oauthLoginSuccessHandler,
            RegistrationService registrationService,
            JwtService jwtService,
            UserRepository userRepository,
            RedisRefreshTokenStore refreshTokenStore,
            WebClientOrigins webClientOrigins
    ) {
        this.oauthUserService = oauthUserService;
        this.oauthLoginSuccessHandler = oauthLoginSuccessHandler;
        this.registrationService = registrationService;
        this.webClientOrigins = webClientOrigins;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationRequestResolver authorizationRequestResolver
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/.env", "/.git/**",
                                "/application.yaml", "/application.yml",
                                "/application-*.yaml", "/application-*.yml"
                        ).denyAll()
                        .requestMatchers(
                                "/login", "/oauth2/**", "/login/oauth2/**",
                                "/api/japan-postal-codes",
                                "/api/auth/csrf", "/api/auth/refresh", "/api/auth/oauth/complete",
                                "/logout", "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/detail").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/detail").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/terms").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/terms").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**", "/api/mypage/**", "/signup/**", "/mypage/**").authenticated()
                        .anyRequest().denyAll()
                )
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getServletPath().startsWith("/api/auth/")
                                || request.getServletPath().startsWith("/api/mypage")
                                || request.getServletPath().startsWith("/api/admin/")
                ))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver))
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauthUserService))
                        .successHandler(oauthLoginSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            log.error("OAuth login failed: {}", exception.getMessage(), exception);
                            response.sendRedirect(webClientOrigins.clientBaseUrl()
                                    + "/login?error&reason=oauth-failed");
                        })
                )
                .logout(logout -> logout.disable())
                .addFilterAfter(
                        new RegistrationCompletionFilter(registrationService),
                        OAuth2LoginAuthenticationFilter.class
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, userRepository, refreshTokenStore),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        new AdminSessionAuthenticationFilter(),
                        JwtAuthenticationFilter.class
                )
                .addFilterBefore(
                        new AllowedOriginFilter(webClientOrigins.allowedOrigins()),
                        LogoutFilter.class
                );

        return http.build();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new OAuthReauthenticationRequestResolver(clientRegistrationRepository);
    }

    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwsAlgorithmResolver(clientRegistration ->
                "line".equals(clientRegistration.getRegistrationId())
                        ? MacAlgorithm.HS256
                        : SignatureAlgorithm.RS256
        );
        return factory;
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(webClientOrigins.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Accept", "Authorization", "Content-Type", "X-XSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
