package com.yamoyo.be.domain.teamroom.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateTeamRoomRequest (
    @NotBlank(message = "팀룸 제목은 필수입니다")
    @Size(max = 20, message = "제목은 20자 이내여야 합니다.")
    String title,

    @Size(max = 50, message = "설명은 최대 50자까지 입력할 수 있습니다.")
    String description,

    @NotNull(message = "마감일은 필수입니다")
    @Future(message = "마감일은 현재 시간 이후여야 합니다")
    LocalDateTime deadline,

    Long bannerImageId
){}
