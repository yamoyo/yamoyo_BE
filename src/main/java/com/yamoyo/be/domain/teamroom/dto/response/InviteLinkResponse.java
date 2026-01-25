package com.yamoyo.be.domain.teamroom.dto.response;

public record InviteLinkResponse(
        String token,
        long expiresInSeconds
) {}
