package com.yamoyo.be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Onboarding
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "약관을 찾을 수 없습니다."),
    MANDATORY_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 합니다."),
    TERMS_NOT_AGREED(HttpStatus.FORBIDDEN, "약관에 동의하지 않아 프로필 설정을 진행할 수 없습니다."),

    // Auth
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    INVALID_ACCESSTOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    // Timepick
    TIMEPICK_NOT_FOUND(HttpStatus.NOT_FOUND, "타임픽을 찾을 수 없습니다."),
    TIMEPICK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 타임픽이 존재합니다."),
    TIMEPICK_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "타임픽 참가자가 아닙니다."),
    TIMEPICK_NOT_OPEN(HttpStatus.BAD_REQUEST, "타임픽이 진행 중이 아닙니다."),
    TIMEPICK_AVAILABILITY_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "이미 가용시간을 제출했습니다."),
    TIMEPICK_PREFERRED_BLOCK_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "이미 선호시간대를 제출했습니다."),
    TIMEPICK_ALREADY_FINALIZED(HttpStatus.BAD_REQUEST, "이미 마감된 타임픽입니다."),

    // Everytime
    EVERYTIME_INVALID_URL(HttpStatus.BAD_REQUEST, "올바른 에브리타임 URL 형식이 아닙니다."),
    EVERYTIME_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "에브리타임 시간표 파싱에 실패했습니다."),
    EVERYTIME_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "에브리타임 API 호출에 실패했습니다."),
    EVERYTIME_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "에브리타임 서버 응답 시간이 초과되었습니다."),

    // TeamRoom
    INVALID_DEADLINE(HttpStatus.BAD_REQUEST, "마감일은 현재 날짜 +1일 이후여야 합니다."),
    TEAMROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 팀룸을 찾을 수 없습니다."),
    INVITE_INVALID(HttpStatus.GONE, "만료되거나 유효하지 않은 초대링크입니다."),
    TEAMROOM_JOIN_FORBIDDEN(HttpStatus.FORBIDDEN, "팀룸 입장이 불가합니다."),
    TEAMROOM_FULL(HttpStatus.BAD_REQUEST, "이미 꽉 찬 팀룸입니다."),
    SETUP_NOT_FOUND(HttpStatus.NOT_FOUND, "팀룸 설정 정보를 찾을 수 없습니다."),

    // TeamMember
    NOT_TEAM_MEMBER(HttpStatus.FORBIDDEN, "팀룸의 팀원이 아닙니다."),
    TEAMROOM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 팀룸에서 팀원을 찾을 수 없습니다."),
    NOT_TEAM_MANAGER(HttpStatus.FORBIDDEN, "관리자 권한이 없습니다."),
    TEAMROOM_LEAVE_FORBIDDEN(HttpStatus.BAD_REQUEST, "현재 팀룸을 나갈 수 없습니다."),
    TEAMROOM_KICK_FORBIDDEN(HttpStatus.BAD_REQUEST, "진행 중에는 팀원을 강퇴할 수 없습니다."),
    CANNOT_DELEGATE_TO_SELF(HttpStatus.BAD_REQUEST, "팀장/방장은 자기 자신에게 위임할 수 없습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 강퇴할 수 없습니다."),
    CANNOT_KICK_MANAGER(HttpStatus.BAD_REQUEST, "관리자는 강퇴할 수 없습니다."),
    BANNED_MEMBER(HttpStatus.FORBIDDEN, "해당 팀룸에서 강퇴된 사용자입니다."),

    // LeaderGame
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "게임을 찾을 수 없습니다."),
    GAME_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 게임이 진행 중입니다."),
    NOT_ALL_MEMBERS_CONNECTED(HttpStatus.BAD_REQUEST, "모든 멤버가 접속하지 않았습니다."),
    NOT_ROOM_HOST(HttpStatus.FORBIDDEN, "방장만 수행할 수 있습니다."),
    INVALID_GAME_PHASE(HttpStatus.BAD_REQUEST, "현재 단계에서는 수행할 수 없는 동작입니다."),
    TIMING_ALREADY_STOPPED(HttpStatus.BAD_REQUEST, "이미 타이밍을 기록했습니다."),
    ROOM_NOT_IN_LEADER_SELECTION(HttpStatus.BAD_REQUEST, "팀장 선출 단계가 아닙니다."),
    ROOM_NOT_JOINABLE(HttpStatus.BAD_REQUEST, "현재 팀룸에 입장할 수 없는 상태입니다."),
    ALREADY_VOLUNTEER(HttpStatus.BAD_REQUEST, "이미 투표했습니다."),

    // Meeting
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "회의를 찾을 수 없습니다."),
    MEETING_INVALID_START_TIME(HttpStatus.BAD_REQUEST, "시작 시간은 30분 단위여야 합니다."),
    MEETING_INVALID_DURATION(HttpStatus.BAD_REQUEST, "회의 시간은 30분 단위여야 합니다."),
    MEETING_PURPLE_COLOR_FORBIDDEN(HttpStatus.BAD_REQUEST, "PURPLE 색상은 정기 회의 전용입니다."),
    MEETING_PARTICIPANT_NOT_TEAM_MEMBER(HttpStatus.BAD_REQUEST, "참석자 중 팀원이 아닌 사용자가 있습니다."),
    MEETING_INVALID_YEAR_MONTH_PARAMETER(HttpStatus.BAD_REQUEST, "year와 month는 둘 다 입력해야 합니다."),
    MEETING_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "회의 수정 권한이 없습니다."),
    MEETING_COLOR_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "최초 정기회의의 색상은 변경할 수 없습니다."),
    MEETING_INVALID_UPDATE_SCOPE(HttpStatus.BAD_REQUEST, "일회성 회의에는 해당 수정 범위를 사용할 수 없습니다."),
    MEETING_SINGLE_SCOPE_REQUIRES_DATE(HttpStatus.BAD_REQUEST, "단일 회의 수정 시 날짜(date)는 필수입니다."),
    MEETING_FUTURE_SCOPE_REQUIRES_DAY_OF_WEEK(HttpStatus.BAD_REQUEST, "이후 회의 수정 시 요일(dayOfWeek)은 필수입니다."),
    MEETING_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "회의 삭제 권한이 없습니다."),
    MEETING_INVALID_DELETE_SCOPE(HttpStatus.BAD_REQUEST, "일회성 회의에는 해당 삭제 범위를 사용할 수 없습니다."),
    MEETING_DURATION_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "회의 시간은 30분 이상 240분(4시간) 이하여야 합니다."),

    // Team Rule & Collaboration Tool
    RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 규칙을 찾을 수 없습니다."),
    TOOL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 협업툴을 찾을 수 없습니다."),
    ALREADY_VOTED(HttpStatus.BAD_REQUEST, "이미 투표를 완료했습니다."),
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 제안을 찾을 수 없습니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 대한 접근 권한이 없습니다."),

    // Server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");


    private final HttpStatus httpStatus;
    private final String message;
}
