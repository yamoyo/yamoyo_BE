package com.yamoyo.be.event.event;

public record MemberRemovedEvent(
        Long roomId,
        Long userId,
        String type // LEAVE 또는 KICK
) {
}
