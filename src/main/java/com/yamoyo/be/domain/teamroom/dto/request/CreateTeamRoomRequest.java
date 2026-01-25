package com.yamoyo.be.domain.teamroom.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateTeamRoomRequest (
    @NotBlank(message = "팀룸 제목은 필수입니다")
    String title,

    String description,

    @NotNull(message = "마감일은 필수입니다")
    @Future(message = "마감일은 현재 시간 이후여야 합니다")
    LocalDateTime deadline,

    Long bannerImageId
){}
