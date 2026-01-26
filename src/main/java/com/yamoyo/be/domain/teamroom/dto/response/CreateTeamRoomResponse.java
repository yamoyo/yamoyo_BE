package com.yamoyo.be.domain.teamroom.dto.response;

import java.time.LocalDateTime;

public record CreateTeamRoomResponse(
        Long teamRoomId,
        String inviteToken,
        Long expiresInSeconds
) {}
