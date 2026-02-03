package com.yamoyo.be.domain.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    // 팀룸
    TEAM_JOIN("[%s] 팀룸 참여", "새로운 팀원이 참여했습니다. 함께 인사하여 프로젝트를 시작해보세요!"),             // 팀원 신규 참여
    TEAM_LEADER_CONFIRM("[%s] 팀장 확정 알림", "%s 팀의 리더가 최종 확정되었습니다."),   // 팀장 선정 완료
    TEAM_LEADER_CHANGE("[%s] 팀장 변경 알림", "%s 팀의 리더가 변경되었습니다."),      // 팀장 변경 완료
    TEAM_DEADLINE_REMIND("[%s] 팀룸 마감 D-1", "마감까지 단 하루! 마지막으로 놓친 부분은 없는지 점검해보세요."),   // 팀룸 마감 1일 전
    TEAM_ARCHIVED("[%s] 팀룸 아카이빙 알림", "%s 팀의 프로젝트가 종료되어 아카이빙되었습니다. 그동안의 기록을 추억해보세요."),     // 마감 7일 후 아카이빙

    // 규칙 및 회의 관련
    RULE_CONFIRM("[%s] 규칙 확정 안내", "%s 팀의 기본 규칙이 최종 확정되었습니다."),   // 규칙 및 정기회의 모두 확정
    RULE_CHANGE("[%s] 규칙 변경 알림", "%s 팀의 규칙이 업데이트되었습니다. 변경된 내용을 확인하고 피드백을 남겨주세요!"),           // 규칙 추가/수정/삭제
    MEETING_CHANGE("[%s] 회의 변경 알림", "%s 팀의 회의 일정이 변경되었습니다. 캘린더를 확인하여 스케줄을 조정해주세요."),        // 회의 추가/수정/삭제
    MEETING_REMIND("[%s] 회의 10분 전 리마인드", "%s 팀의 회의 시간 10분 전입니다! 잊지 말고 회의실로 입장해주세요."),  // 회의 시작 10분 전

    // 제안 알림
    TOOL_SUGGESTION("[%s] 협업 툴 제안", "새로운 협업 툴 제안이 도착했습니다. 확인 후 승인 여부를 결정해주세요."),           // 제안 즉시 (팀장에게만)
    TOOL_APPROVED("[%s] 제안 승인 알림", "제안하신 협업 툴이 승인되었습니다. 이제 팀룸에서 함께 사용할 수 있습니다!"),         // 승인 즉시 (팀원 전원)
    TOOL_REJECTED("[%s] 제안 반려 알림", "제안하신 협업 툴이 반려되었습니다. 대시보드에서 상세 내용을 확인해주세요.");         // 반려 즉시 (제안한 팀원에게만)

    private final String title;
    private final String message;

    public String generateTitle(String teamRoomName) {
        return String.format(this.title, teamRoomName);
    }
    public String generateDescription(String teamRoomName) {
        return String.format(this.message, teamRoomName);
    }
}
