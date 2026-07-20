package com.zik00.shop.config;

import com.zik00.admin.config.AdminSessionAuthenticationFilter;
import com.zik00.shop.service.auth.GoogleOAuth2UserService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.JwtService;
import com.zik00.shop.service.auth.RedisRefreshTokenStore;
import com.zik00.shop.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
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

@Configuration
public class SecurityConfig {
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;
    private final RegistrationService registrationService;
    private final WebClientOrigins webClientOrigins;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RedisRefreshTokenStore refreshTokenStore;

    public SecurityConfig(
            GoogleOAuth2UserService googleOAuth2UserService,
            GoogleLoginSuccessHandler googleLoginSuccessHandler,
            RegistrationService registrationService,
            JwtService jwtService,
            UserRepository userRepository,
            RedisRefreshTokenStore refreshTokenStore,
            WebClientOrigins webClientOrigins
    ) {
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.googleLoginSuccessHandler = googleLoginSuccessHandler;
        this.registrationService = registrationService;
        this.webClientOrigins = webClientOrigins;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**", "/api/mypage/**", "/signup/**", "/mypage/**").authenticated()
                        .anyRequest().denyAll()
                )
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getServletPath().startsWith("/api/auth/")
                                || request.getServletPath().startsWith("/api/mypage")
                                || request.getServletPath().startsWith("/api/admin/")
                ))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(googleOAuth2UserService))
                        .successHandler(googleLoginSuccessHandler)
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect(webClientOrigins.clientBaseUrl() + "/login?error"))
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
