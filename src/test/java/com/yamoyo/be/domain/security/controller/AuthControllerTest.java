package com.yamoyo.be.domain.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamoyo.be.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.domain.security.service.AuthService;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 *
 * 테스트 내용:
 * 1. POST /api/auth/refresh - Access Token 재발급 API 테스트
 * 2. RefreshToken 쿠키 처리 테스트
 * 3. 에러 응답 테스트
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

    private static final String REFRESH_ENDPOINT = "/api/auth/refresh";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    @Test
    @DisplayName("POST /api/auth/refresh - 유효한 RefreshToken으로 AccessToken 재발급 성공")
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
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value(NEW_ACCESS_TOKEN))
                .andExpect(jsonPath("$.accessTokenExpiration").value(600000))
                .andReturn();

        // 새로운 RefreshToken이 쿠키로 설정되었는지 확인
        Cookie newRefreshTokenCookie = result.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);
        assertThat(newRefreshTokenCookie).isNotNull();
        assertThat(newRefreshTokenCookie.getValue()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(newRefreshTokenCookie.isHttpOnly()).isTrue();

        verify(authService).refresh(OLD_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("POST /api/auth/refresh - RefreshToken 쿠키가 없는 경우 400 에러")
    void refresh_NoRefreshTokenCookie_BadRequest() throws Exception {
        // when & then
        mockMvc.perform(post(REFRESH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh - 유효하지 않은 RefreshToken인 경우 401 에러")
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
    @DisplayName("POST /api/auth/refresh - 만료된 RefreshToken인 경우 401 에러")
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
    @DisplayName("POST /api/auth/refresh - Response Body에 RefreshToken이 포함되지 않음")
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
                .andExpect(jsonPath("$.refreshToken").doesNotExist()); // refreshToken은 body에 포함되지 않아야 함
    }

    @Test
    @DisplayName("POST /api/auth/refresh - 새로운 RefreshToken이 HttpOnly 쿠키로 설정됨")
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

        // then - 새로운 RefreshToken이 HttpOnly 쿠키로 설정되었는지 확인
        Cookie newCookie = result.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);
        assertThat(newCookie).isNotNull();
        assertThat(newCookie.getValue()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(newCookie.isHttpOnly()).isTrue();
        assertThat(newCookie.getPath()).isEqualTo("/");
    }
}
