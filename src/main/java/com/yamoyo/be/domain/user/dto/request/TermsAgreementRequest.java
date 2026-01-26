package com.yamoyo.be.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 약관 동의 요청 DTO
 *
 * Role:
 * - 사용자의 약관 동의 정보를 전달하는 요청 객체
 * - 온보딩 과정에서 필수 약관(서비스 이용약관, 개인정보 처리방침) 동의에 사용
 *
 * @param agreements 동의한 약관 목록
 */
public record TermsAgreementRequest(
        @NotNull(message = "약관 동의 목록은 필수입니다.")
        List<TermAgreement> agreements
) {
    /**
     * 개별 약관 동의 정보
     *
     * @param termsId 약관 ID
     * @param agreed 동의 여부 (true: 동의, false: 미동의)
     */
    public record TermAgreement(
            @NotNull(message = "약관 ID는 필수입니다.")
            Long termsId,

            @NotNull(message = "동의 여부는 필수입니다.")
            Boolean agreed
    ) {}
}
