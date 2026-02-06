package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.request.EverytimeParseRequest;
import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.service.TimepickService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Everytime", description = "에브리타임 연동 API")
@Slf4j
@RestController
@RequestMapping("/api/everytime")
@RequiredArgsConstructor
public class EverytimeController {

    private final TimepickService timepickService;

    @Operation(
            summary = "에브리타임 시간표 파싱",
            description = "에브리타임 공개 URL을 파싱하여 가능 시간을 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "파싱 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 URL 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "파싱 실패")
    })
    @PostMapping("/parse")
    public ApiResponse<AvailabilityResponse> parse(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody EverytimeParseRequest request
    ) {
        log.info("에브리타임 파싱 요청 - UserId: {}", claims.userId());
        AvailabilityResponse response = timepickService.parseEverytime(request.url());
        return ApiResponse.success(response);
    }
}
