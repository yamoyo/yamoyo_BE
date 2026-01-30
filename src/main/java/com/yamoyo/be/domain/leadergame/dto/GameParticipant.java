package com.yamoyo.be.domain.leadergame.dto;

public record GameParticipant (
    Long userId,
    String name,
    String profileImageId
)
{
    public static GameParticipant of(Long userId, String name, String profileImageId) {
        return new GameParticipant(userId, name, profileImageId);
    }
}