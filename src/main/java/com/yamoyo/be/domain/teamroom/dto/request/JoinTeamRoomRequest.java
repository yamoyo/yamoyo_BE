package com.yamoyo.be.domain.teamroom.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JoinTeamRoomRequest (
        @NotBlank(message = "초대링크는 필수입니다")
        String inviteToken
){}
