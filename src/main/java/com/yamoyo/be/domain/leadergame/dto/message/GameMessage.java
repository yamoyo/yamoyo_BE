package com.yamoyo.be.domain.leadergame.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage<T> {
    private String type;
    private T payload;

    public static <T> GameMessage<T> of(String type, T payload) {
        return new GameMessage<>(type, payload);
    }
}