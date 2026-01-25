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
        // 프로필 설정 API에 대해 온보딩 Interceptor 적용
        // 약관 미동의 사용자의 접근 차단
        registry.addInterceptor(onboardingInterceptor)
                .addPathPatterns("/api/users/profile");
    }
}
