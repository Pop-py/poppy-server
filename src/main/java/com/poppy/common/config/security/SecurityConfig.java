package com.poppy.common.config.security;

import com.poppy.common.auth.filter.JwtAuthenticationFilter;
import com.poppy.common.auth.handler.OAuth2LoginFailureHandler;
import com.poppy.common.auth.handler.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(configurationSource()))  // cors 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/", "/token", "/refresh").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()   // Swagger API
                        .requestMatchers("/popup-stores/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reviews/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/ws/**", "/ws/notification/**", "/queue/**").permitAll()
                        .requestMatchers("/popup-stores/{storeId}/waiting/**").hasRole("MASTER")  // 관리자용 API
                        .requestMatchers("/users/{id}/popup-stores/{storeId}/waiting/**").hasRole("USER")  // 사용자용 API
                        .requestMatchers("/users/{id}/notifications").hasRole("USER")
                        .requestMatchers("/users/{id}/notification/{notificationId}").hasRole("USER")
                        .requestMatchers("/payments/**").permitAll()    // 토스페이먼츠 관련
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())  // form 로그인 비활성화
                .httpBasic(httpBasic -> httpBasic.disable()) // Header에 로그인 정보를 담는 방식 비활성화 (Bearer 방식을 사용하기 때문)
                .oauth2Login(oauth -> oauth
                        // oauth2 인증 서버로 리다이렉션하기 위한 엔드포인트
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization")
                        )
                        // 사용자가 네이버 로그인 페이지로 이동하도록하는 단계
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*"))
                        .successHandler(oauth2LoginSuccessHandler)
                        .failureHandler(oauth2LoginFailureHandler)
                )
                .addFilterAfter(jwtAuthenticationFilter, OAuth2LoginAuthenticationFilter.class) // JWT 필터
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);    // 인증 정보 포함 가능
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));    // 헤더 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
