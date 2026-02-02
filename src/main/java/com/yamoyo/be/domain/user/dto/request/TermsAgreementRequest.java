package com.yamoyo.be.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "약관 동의 요청")
public record TermsAgreementRequest(
        @Schema(description = "동의한 약관 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "약관 동의 목록은 필수입니다.")
        List<TermAgreement> agreements
) {
    @Schema(description = "개별 약관 동의 정보")
    public record TermAgreement(
            @Schema(description = "약관 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "약관 ID는 필수입니다.")
            Long termsId,

            @Schema(description = "동의 여부 (true: 동의, false: 미동의)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "동의 여부는 필수입니다.")
            Boolean agreed
    ) {}
}
