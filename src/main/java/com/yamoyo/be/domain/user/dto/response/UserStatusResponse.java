package com.yamoyo.be.domain.user.dto.response;

public record UserStatusResponse(
        String type,
        Long userId,
        String username,
        Long profileImageId,
        String status
) {
}
