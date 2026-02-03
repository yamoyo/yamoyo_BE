package com.yamoyo.be.config;

import com.yamoyo.be.domain.security.handler.CustomLogoutSuccessHandler;
import com.yamoyo.be.domain.security.jwt.filter.JwtAuthenticationFilter;
import com.yamoyo.be.domain.security.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.yamoyo.be.domain.security.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration
 *
 * Role:
 * - Spring Security 설정을 담당하는 클래스
 * - OAuth2 로그인과 JWT 인증을 모두 지원하는 하이브리드 인증 시스템 구성
 *
 * Complexity/Rationale:
 * 1. 하이브리드 인증 시스템:
 *    - OAuth2 로그인: Google, Kakao 등 소셜 로그인
 *    - JWT 인증: OAuth2 로그인 성공 후 JWT 토큰 발급, 이후 API 요청 시 JWT로 인증
 *
 * 2. 인증 플로우:
 *    a) 최초 로그인:
 *       - 사용자가 /oauth2/authorization/{provider} 접근
 *       - OAuth2 Provider로 리다이렉트하여 로그인
 *       - CustomOAuth2UserService가 사용자 정보 로드
 *       - OAuth2AuthenticationSuccessHandler가 JWT 토큰 발급
 *       - 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
 *
 *    b) 이후 API 요청:
 *       - 클라이언트가 Authorization: Bearer {token} 헤더로 요청
 *       - JwtAuthenticationFilter가 토큰 검증 및 SecurityContext 설정
 *       - Controller에서 @AuthenticationPrincipal로 사용자 정보 접근
 *
 * 3. 세션 관리:
 *    - SessionCreationPolicy.STATELESS: JWT 기반 인증은 stateless
 *    - 서버는 세션을 생성하지 않고 JWT 토큰으로만 인증 상태 관리
 *
 * 4. 보안 설정:
 *    - CSRF 비활성화: Stateless JWT 인증에서는 CSRF 토큰 불필요
 *    - /api/auth/refresh는 인증 없이 접근 가능 (Refresh Token으로 검증)
 *    - 그 외 API는 JWT 인증 필요
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    // 인증 없이 접근을 허용할 경로 목록
    private static final String[] PERMIT_ALL_PATTERNS = {
        // 로그인 페이지 및 정적 리소스
        "/login", "/css/**", "/images/**", "/js/**", "/favicon.ico", "/error",

        // 인증 관련 (Refresh Token 재발급 등)
        "/api/auth/refresh",

        // 시스템 관리 (Health Check)
        "/api/actuator/health",

        // WebSocket 엔드포인트
        "/ws/**",

        // 테스트 페이지
        "/test/**",
        "/api/actuator/health",

        // Swagger UI
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    /**
     * Spring Security Filter Chain 구성
     *
     * Role:
     * - HTTP 요청에 대한 보안 정책 설정
     * - OAuth2 로그인 + JWT 인증 통합
     *
     * Complexity/Rationale:
     * 1. CSRF 비활성화:
     *    - JWT 기반 인증은 stateless이므로 CSRF 공격에 안전
     *    - 쿠키를 사용하지 않고 Authorization 헤더로 토큰 전달
     *
     * 2. 세션 관리:
     *    - STATELESS: 서버는 세션을 생성하지 않음
     *    - JWT 토큰으로만 인증 상태 관리
     *
     * 3. 권한 설정:
     *    - /login, /api/auth/refresh: 인증 없이 접근 가능
     *    - 그 외 요청: JWT 인증 필요
     *
     * 4. OAuth2 로그인:
     *    - CustomOAuth2UserService: 사용자 정보 로드 및 DB 저장
     *    - OAuth2AuthenticationSuccessHandler: JWT 토큰 발급 및 리다이렉트
     *
     * 5. JWT 필터:
     *    - JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 전에 추가
     *    - Authorization 헤더에서 JWT 토큰 추출 및 검증
     *    - 유효한 토큰이면 SecurityContext에 인증 정보 설정
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain
     * @throws Exception 설정 오류 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 0. CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 1. CSRF 비활성화
            // JWT 기반 stateless 인증에서는 CSRF 토큰 불필요
            .csrf(csrf -> csrf.disable())

            // 2. 세션 관리 정책: STATELESS
            // 서버는 세션을 생성하지 않고 JWT 토큰으로만 인증 상태 관리
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 3. 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
                // 그 외 모든 요청은 JWT 인증 필요
                .anyRequest().authenticated()
            )

            // 4. OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                // 커스텀 로그인 페이지 경로 지정
                .loginPage("/login")
                // 사용자 정보 처리 서비스 등록
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                // OAuth2 로그인 성공 시 JWT 토큰 발급 및 리다이렉트
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )

            // 5. 로그아웃 설정
            // Spring Security LogoutFilter가 처리하는 로그아웃 엔드포인트
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "refresh_token")
            )
            
            // 6. 예외 처리
            // /api/** 경로에 대해 인증되지 않은 요청 시 401 에러 반환 (로그인 페이지 리다이렉트 방지)
            .exceptionHandling(exception -> exception
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getServletPath().startsWith("/api/")
                )
            )

            // 7. JWT 인증 필터 추가
            // UsernamePasswordAuthenticationFilter 전에 JwtAuthenticationFilter 실행
            // Authorization 헤더에서 JWT 토큰을 추출하여 검증
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드(3000)와 백엔드(8080) 허용
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 쿠키 및 인증 헤더 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
