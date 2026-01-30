package com.yamoyo.be.domain.leadergame.enums;

public enum GamePhase {
    VOLUNTEER,      // 팀장 지원 (10초)
    GAME_SELECT,    // 방장의 게임 선택
    GAME_READY,     // 게임 시작 전 대기
    GAME_PLAYING,   // 실제 게임 진행
    RESULT          // 결과 발표
}