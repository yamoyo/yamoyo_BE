package com.yamoyo.be.oauth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home & Login Controller
 *
 * Role:
 * - 메인 페이지 및 로그인 페이지를 처리하는 컨트롤러
 * - OAuth2 인증된 사용자 정보를 뷰로 전달
 *
 * Complexity/Rationale:
 * 1. OAuth2User Principal:
 *    - Spring Security OAuth2가 인증 완료 후 OAuth2User 객체를 생성
 *    - 이 객체는 Google UserInfo API에서 받은 사용자 정보를 담고 있음
 *    - @AuthenticationPrincipal 어노테이션으로 현재 인증된 사용자 정보에 접근
 *
 * 2. Attributes 구조:
 *    - OAuth2User.getAttributes()는 Map<String, Object> 형태
 *    - Google Provider의 경우 주요 필드:
 *      * sub: Google 사용자 고유 ID
 *      * name: 사용자 이름
 *      * email: 이메일 주소
 *      * picture: 프로필 이미지 URL
 *      * email_verified: 이메일 인증 여부
 *
 * 3. View Rendering:
 *    - Thymeleaf 템플릿 엔진을 사용하여 사용자 정보를 HTML로 렌더링
 *    - Model 객체에 사용자 정보를 담아 뷰로 전달
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(OAuth2AuthenticationToken authentication, Model model) {
        if (authentication != null) {
            OAuth2User principal = authentication.getPrincipal();

            // 1. 어떤 Provider로 로그인했는지 확인 (google, kakao)
            String registrationId = authentication.getAuthorizedClientRegistrationId();
            String provider = registrationId.equalsIgnoreCase("kakao") ? "Kakao" : "Google";
            model.addAttribute("provider", provider);

            // Google OAuth2 사용자 정보 추출
            String name = principal.getAttribute("name");
            String email = principal.getAttribute("email");
            String picture = principal.getAttribute("picture");

            // 모델에 사용자 정보 추가 (Thymeleaf 템플릿에서 사용)
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("picture", picture);
        }

        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
