package com.yamoyo.be.domain.user.dto.response;

public record UserStatusResponse(
        String type,
        Long userId,
        Long profileImageId,
        String status
) {
}
