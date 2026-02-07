package com.yamoyo.be.config;

import com.yamoyo.be.common.interceptor.OnboardingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 *
 * Role:
 * - Spring MVC 관련 설정
 * - Interceptor 등록
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final OnboardingInterceptor onboardingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 온보딩 완료 전에는 핵심 API 접근 차단
        // 인증/온보딩/헬스체크 등은 예외로 허용
        registry.addInterceptor(onboardingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/onboarding/**",
                        "/api/actuator/health"
                );
    }
}
