package com.yamoyo.be.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";
    private static final String OAUTH2_TAG = "OAuth2 Login";

    static {
        SpringDocUtils.getConfig().replaceWithSchema(
                LocalTime.class,
                new Schema<LocalTime>()
                        .type("string")
                        .format("time")
                        .example("14:30:00")
        );
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()))
                .tags(List.of(
                        new Tag().name(OAUTH2_TAG).description("OAuth2 소셜 로그인 API (Spring Security 제공)")
                ))
                .paths(oAuth2Paths());
    }

    private Info apiInfo() {
        return new Info()
                .title("Yamoyo API")
                .version("1.0.0")
                .description("Yamoyo 백엔드 API 문서");
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.");
    }

    private Paths oAuth2Paths() {
        Paths paths = new Paths();

        // Google OAuth2 로그인
        paths.addPathItem("/oauth2/authorization/google", new PathItem()
                .get(new Operation()
                        .tags(List.of(OAUTH2_TAG))
                        .summary("Google 소셜 로그인")
                        .description("""
                                Google OAuth2 로그인을 시작합니다.

                                **플로우:**
                                1. 이 엔드포인트로 리다이렉트
                                2. Google 로그인 페이지로 자동 이동
                                3. 사용자가 Google 계정으로 로그인
                                4. 로그인 성공 시 프론트엔드로 리다이렉트 (JWT 토큰 포함)

                                **리다이렉트 URL 예시:**
                                - 신규 사용자: `{frontend_url}/onboarding/terms?access_token=xxx`
                                - 기존 사용자: `{frontend_url}/home?access_token=xxx`
                                """)
                        .responses(new ApiResponses()
                                .addApiResponse("302", new ApiResponse()
                                        .description("Google 로그인 페이지로 리다이렉트")))));

        // Kakao OAuth2 로그인
        paths.addPathItem("/oauth2/authorization/kakao", new PathItem()
                .get(new Operation()
                        .tags(List.of(OAUTH2_TAG))
                        .summary("Kakao 소셜 로그인")
                        .description("""
                                Kakao OAuth2 로그인을 시작합니다.

                                **플로우:**
                                1. 이 엔드포인트로 리다이렉트
                                2. Kakao 로그인 페이지로 자동 이동
                                3. 사용자가 Kakao 계정으로 로그인
                                4. 로그인 성공 시 프론트엔드로 리다이렉트 (JWT 토큰 포함)

                                **리다이렉트 URL 예시:**
                                - 신규 사용자: `{frontend_url}/onboarding/terms?access_token=xxx`
                                - 기존 사용자: `{frontend_url}/home?access_token=xxx`
                                """)
                        .responses(new ApiResponses()
                                .addApiResponse("302", new ApiResponse()
                                        .description("Kakao 로그인 페이지로 리다이렉트")))));

        // 로그아웃
        paths.addPathItem("/api/auth/logout", new PathItem()
                .post(new Operation()
                        .tags(List.of("Auth"))
                        .summary("로그아웃")
                        .description("""
                                사용자 로그아웃을 처리합니다.

                                **처리 내용:**
                                - 세션 무효화
                                - JSESSIONID, refresh_token 쿠키 삭제
                                - DB의 Refresh Token 삭제

                                **주의:** Access Token은 stateless이므로 만료 시까지 유효합니다.
                                """)
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .description("로그아웃 성공")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType()
                                                        .schema(new Schema<>()
                                                                .type("object")
                                                                .addProperty("message", new Schema<>()
                                                                        .type("string")
                                                                        .example("로그아웃 성공"))))))
                                .addApiResponse("401", new ApiResponse()
                                        .description("인증 실패")))));

        return paths;
    }
}
