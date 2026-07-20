package com.zik00.shop.config;

import com.zik00.shop.service.auth.GoogleOAuth2UserService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.JwtCookieService;
import com.zik00.shop.service.auth.JwtService;
import com.zik00.shop.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;
    private final RegistrationService registrationService;
    private final String frontendBaseUrl;
    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;
    private final UserRepository userRepository;

    public SecurityConfig(
            GoogleOAuth2UserService googleOAuth2UserService,
            GoogleLoginSuccessHandler googleLoginSuccessHandler,
            RegistrationService registrationService,
            JwtService jwtService,
            JwtCookieService jwtCookieService,
            UserRepository userRepository,
            @Value("${shop.frontend.base-url:http://localhost:5174}") String frontendBaseUrl
    ) {
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.googleLoginSuccessHandler = googleLoginSuccessHandler;
        this.registrationService = registrationService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.jwtService = jwtService;
        this.jwtCookieService = jwtCookieService;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/admin/**")
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/.env", "/.git/**",
                                "/application.yaml", "/application.yml",
                                "/application-*.yaml", "/application-*.yml"
                        ).denyAll()
                        .requestMatchers(
                                "/login", "/oauth2/**", "/login/oauth2/**",
                                "/css/**", "/js/**", "/api/japan-postal-codes",
                                "/api/auth/csrf", "/api/auth/refresh",
                                "/admin/**", "/api/admin/**", "/", "/index.html", "/error"
                        ).permitAll()
                        .requestMatchers("/api/auth/**", "/signup/**", "/mypage/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getServletPath().startsWith("/api/auth/")
                ))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(googleOAuth2UserService))
                        .successHandler(googleLoginSuccessHandler)
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect(frontendBaseUrl + "/login?error"))
                )
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.sendRedirect(frontendBaseUrl + "/login?logout"))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                .addFilterAfter(
                        new RegistrationCompletionFilter(registrationService),
                        OAuth2LoginAuthenticationFilter.class
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, jwtCookieService, userRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
