package com.yamoyo.be.domain.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.security.refreshtoken.RefreshTokenRepository;
import com.yamoyo.be.domain.security.service.AuthService;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 *
 * 테스트 내용:
 * 1. POST /api/auth/refresh - Access Token 재발급 API 테스트
 * 2. POST /logout - Spring Security LogoutFilter 테스트
 * 3. RefreshToken 쿠키 처리 테스트
 * 4. 에러 응답 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String PROVIDER = "google";
    private static final String REFRESH_ENDPOINT = "/api/auth/refresh";
    private static final String LOGOUT_ENDPOINT = "/api/auth/logout";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    @Nested
    @DisplayName("POST /api/auth/refresh - Access Token 재발급")
    class RefreshTest {

        @Test
        @DisplayName("유효한 RefreshToken으로 AccessToken 재발급 성공")
        void refresh_ValidRefreshToken_Success() throws Exception {
            // given
            JwtTokenDto newTokens = new JwtTokenDto("Bearer", NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, 600000L);
            given(authService.refresh(OLD_REFRESH_TOKEN)).willReturn(newTokens);

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, OLD_REFRESH_TOKEN);

            // when & then
            MvcResult result = mockMvc.perform(post(REFRESH_ENDPOINT)
                            .cookie(refreshTokenCookie)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"))
                    .andExpect(jsonPath("$.data.accessToken").value(NEW_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.accessTokenExpiration").value(600000))
                    .andReturn();

            // 새로운 RefreshToken이 쿠키로 설정되었는지 확인
            Cookie newRefreshTokenCookie = result.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);
            assertThat(newRefreshTokenCookie).isNotNull();
            assertThat(newRefreshTokenCookie.getValue()).isEqualTo(NEW_REFRESH_TOKEN);
            assertThat(newRefreshTokenCookie.isHttpOnly()).isTrue();

            verify(authService).refresh(OLD_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("RefreshToken 쿠키가 없는 경우 400 에러")
        void refresh_NoRefreshTokenCookie_BadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효하지 않은 RefreshToken인 경우 401 에러")
        void refresh_InvalidRefreshToken_Unauthorized() throws Exception {
            // given
            String invalidToken = "invalid-refresh-token";
            given(authService.refresh(invalidToken)).willThrow(new YamoyoException(ErrorCode.INVALID_REFRESH_TOKEN));

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, invalidToken);

            // when & then
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .cookie(refreshTokenCookie)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("만료된 RefreshToken인 경우 401 에러")
        void refresh_ExpiredRefreshToken_Unauthorized() throws Exception {
            // given
            String expiredToken = "expired-refresh-token";
            given(authService.refresh(expiredToken)).willThrow(new YamoyoException(ErrorCode.INVALID_REFRESH_TOKEN));

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, expiredToken);

            // when & then
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .cookie(refreshTokenCookie)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Response Body에 RefreshToken이 포함되지 않음")
        void refresh_ResponseDoesNotContainRefreshToken() throws Exception {
            // given
            JwtTokenDto newTokens = new JwtTokenDto("Bearer", NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, 600000L);
            given(authService.refresh(OLD_REFRESH_TOKEN)).willReturn(newTokens);

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, OLD_REFRESH_TOKEN);

            // when & then
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .cookie(refreshTokenCookie)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.refreshToken").doesNotExist());
        }

        @Test
        @DisplayName("새로운 RefreshToken이 HttpOnly 쿠키로 설정됨")
        void refresh_NewRefreshTokenSetAsHttpOnlyCookie() throws Exception {
            // given
            JwtTokenDto newTokens = new JwtTokenDto("Bearer", NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, 600000L);
            given(authService.refresh(OLD_REFRESH_TOKEN)).willReturn(newTokens);

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, OLD_REFRESH_TOKEN);

            // when
            MvcResult result = mockMvc.perform(post(REFRESH_ENDPOINT)
                            .cookie(refreshTokenCookie)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            Cookie newCookie = result.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);
            assertThat(newCookie).isNotNull();
            assertThat(newCookie.getValue()).isEqualTo(NEW_REFRESH_TOKEN);
            assertThat(newCookie.isHttpOnly()).isTrue();
            assertThat(newCookie.getPath()).isEqualTo("/api/auth");
        }
    }

    @Nested
    @DisplayName("POST /logout - Spring Security LogoutFilter")
    class LogoutTest {

        @Test
        @DisplayName("인증된 사용자 로그아웃 성공 - DB RefreshToken 삭제 및 쿠키 삭제")
        void logout_AuthenticatedUser_Success() throws Exception {
            // when & then
            MvcResult result = mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andReturn();

            // RefreshToken 쿠키가 삭제되었는지 확인 (maxAge=0)
            Cookie refreshTokenCookie = result.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);
            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.getMaxAge()).isZero();

            // DB에서 RefreshToken 삭제 호출 확인
            verify(refreshTokenRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("인증되지 않은 사용자도 로그아웃 가능 (쿠키만 삭제)")
        void logout_UnauthenticatedUser_Success() throws Exception {
            // when & then
            MvcResult result = mockMvc.perform(post(LOGOUT_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andReturn();

            // RefreshToken 쿠키가 삭제되었는지 확인
            Cookie refreshTokenCookie = result.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);
            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.getMaxAge()).isZero();
        }

        @Test
        @DisplayName("로그아웃 시 JSESSIONID 쿠키도 삭제")
        void logout_DeletesSessionCookie() throws Exception {
            // when
            MvcResult result = mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            // then - JSESSIONID 쿠키 삭제 확인
            Cookie sessionCookie = result.getResponse().getCookie("JSESSIONID");
            assertThat(sessionCookie).isNotNull();
            assertThat(sessionCookie.getMaxAge()).isZero();
        }
    }

    // ========== Helper Methods ==========

    private JwtAuthenticationToken createJwtAuthenticationToken() {
        JwtTokenClaims claims = new JwtTokenClaims(USER_ID, USER_EMAIL, PROVIDER);
        return JwtAuthenticationToken.authenticated(claims);
    }
}
