package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.dto.response.TeamMemberDetailResponse;
import com.yamoyo.be.domain.teamroom.dto.response.TeamMemberListResponse;
import com.yamoyo.be.domain.teamroom.entity.BannedTeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import com.yamoyo.be.domain.teamroom.repository.BannedTeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.event.event.MemberRemovedEvent;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberService {

    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BannedTeamMemberRepository bannedTeamMemberRepository;

    // 실시간 상태 업데이트를 위한 EventPublisher
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 팀룸 나가기
     * - MEMBER: 바로 나가기 처리
     * - HOST 또는 LEADER: 권한 위임 후 나가기 (1명 남으면 나가기 불가)
     * - 팀장 정하기 ~ 규칙/정기회의 확정 전에는 나가기 불가
     */
    @Transactional
    public void leaveTeamRoom(Long teamRoomId, Long userId) {
        log.info("팀룸 나가기 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 팀원 조회
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. Workflow 체크 - PENDING, COMPLETED만 나가기 가능
        if (teamRoom.getWorkflow() == Workflow.LEADER_SELECTION
                || teamRoom.getWorkflow() == Workflow.SETUP) {
            throw new YamoyoException(ErrorCode.TEAMROOM_LEAVE_FORBIDDEN);
        }

        // 4. 1명 남았을 경우 나가기 불가
        long memberCount = teamMemberRepository.countByTeamRoomId(teamRoomId);
        if (memberCount <= 1) {
            throw new YamoyoException(ErrorCode.TEAMROOM_LEAVE_FORBIDDEN);
        }

        // 5. MEMBER면 바로 삭제
        if (member.getTeamRole() == TeamRole.MEMBER) {
            teamMemberRepository.delete(member);
            log.info("팀원 나가기 완료 - memberId: {}", member.getId());
            return;
        }

        // 6. 관리자 권한(HOST 또는 LEADER)이면 권한 위임 후 삭제
        delegateAuthorityRandomly(teamRoomId, member.getId());
        teamMemberRepository.delete(member);
        log.info("관리자 나가기 완료 - 권한 위임됨 - memberId: {}, role: {}",
                member.getId(), member.getTeamRole());

        // 팀룸 나가기 이벤트 발행
        if(teamRoom.getWorkflow() == Workflow.PENDING) {
            eventPublisher.publishEvent(new MemberRemovedEvent(
                    teamRoomId,
                    userId,
                    "LEAVE"));
        }

    }

    /**
     * 팀원 강퇴
     * - HOST 또는 LEADER만 가능
     * - 밴 목록에 추가
     * - 팀장 정하기 ~ 규칙/정기회의 정하기 전까지는 강퇴 불가
     */
    @Transactional
    public void kickMember(Long teamRoomId, Long kickerId, Long targetMemberId) {
        log.info("팀원 강퇴 시작 - teamRoomId: {}, kickerId: {}, targetMemberId: {}",
                teamRoomId, kickerId, targetMemberId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 강퇴 요청자 권한 체크
        TeamMember kicker = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, kickerId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!kicker.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 3. 대상 팀원 조회
        TeamMember target = teamMemberRepository.findById(targetMemberId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 4. 같은 팀룸인지 확인
        if (!target.getTeamRoom().getId().equals(teamRoomId)) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MEMBER);
        }

        // 4-1. 자기 자신은 강퇴 불가
        if (kicker.getId().equals(target.getId())) {
            throw new YamoyoException(ErrorCode.CANNOT_KICK_SELF);
        }

        // 4-2. 타겟이 관리자면 강퇴 금지 (관리자 공백 방지)
        if (target.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.CANNOT_KICK_MANAGER);
        }

        // 5. Workflow 체크 - PENDING, COMPLETED만 강퇴 가능
        if (teamRoom.getWorkflow() == Workflow.LEADER_SELECTION
                || teamRoom.getWorkflow() == Workflow.SETUP) {
            throw new YamoyoException(ErrorCode.TEAMROOM_KICK_FORBIDDEN);
        }

        // 6. 밴 목록 추가
        bannedTeamMemberRepository.save(BannedTeamMember.create(teamRoom, target.getUser()));

        // 7. 팀원 삭제
        teamMemberRepository.delete(target);
        log.info("팀원 강퇴 완료 - targetMemberId: {}", targetMemberId);

        // 이벤트 발행
        if(teamRoom.getWorkflow() == Workflow.PENDING) {
            eventPublisher.publishEvent(new MemberRemovedEvent(
                    teamRoomId,
                    target.getUser().getId(),
                    "KICK"));
        }
    }

    /**
     * 팀장 변경 (명시적 위임)
     * - 현재 HOST 또는 LEADER만 가능
     * - 기존 관리자 → MEMBER, 새 팀장 → LEADER
     */
    @Transactional
    public void changeLeader(Long teamRoomId, Long currentManagerId, Long newManagerMemberId) {
        log.info("팀장 변경 시작 - teamRoomId: {}, currentManagerId: {}, newManagerMemberId: {}",
                teamRoomId, currentManagerId, newManagerMemberId);

        // 1. 팀룸 조회
        teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 현재 관리자 확인 (HOST 또는 LEADER)
        TeamMember currentManager = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, currentManagerId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!currentManager.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 3. 새 팀장 조회
        TeamMember newManager = teamMemberRepository.findById(newManagerMemberId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 4. 같은 팀룸인지 확인
        if (!newManager.getTeamRoom().getId().equals(teamRoomId)) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MEMBER);
        }

        // 5. 자기 자신에게 위임 방지
        if (currentManager.getId().equals(newManager.getId())) {
            throw new YamoyoException(ErrorCode.CANNOT_DELEGATE_TO_SELF);
        }

        // 5. 권한 변경
        TeamRole delegatedRole = currentManager.getTeamRole(); // HOST or LEADER

        currentManager.changeTeamRole(TeamRole.MEMBER);
        newManager.changeTeamRole(delegatedRole);

        log.info("팀장 변경 완료 - oldManagerId: {}, oldRole: {}, newLeaderId: {}",
                currentManager.getId(), currentManager.getTeamRole(), newManager.getId());
    }

    /**
     * 권한 자동 위임 (관리자 나가기 시)
     * - 남은 팀원 중 무작위로 LEADER/HOST 부여
     */
    private void delegateAuthorityRandomly(Long teamRoomId, Long leavingMemberId) {
        // 1. 위임자의 팀 정보 찾기
        TeamMember leavingMember = teamMemberRepository.findById(leavingMemberId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 2. 현재 방의 멤버인지 확인
        if (!leavingMember.getTeamRoom().getId().equals(teamRoomId)) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MEMBER);
        }

        // 3. 위임자 권한 조회 (HOST, LEADER)
        if (!leavingMember.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 4. 위임할 권한 저장
        TeamRole delegatedRole = leavingMember.getTeamRole(); // HOST or LEADER

        // 5. 남은 멤버 목록 조회
        List<TeamMember> remainingMembers = teamMemberRepository.findByTeamRoomId(teamRoomId)
                .stream()
                .filter(member -> !member.getId().equals(leavingMemberId))
                .toList();

        // 6. 남은 멤버가 없을 경우 불가능
        if (remainingMembers.isEmpty()) {
            throw new YamoyoException(ErrorCode.TEAMROOM_LEAVE_FORBIDDEN);
        }

        // 7. 권한 위임 (대상은 랜덤으로 지정)
        TeamMember newManager = remainingMembers.get(
                ThreadLocalRandom.current().nextInt(remainingMembers.size())
        );
        newManager.changeTeamRole(delegatedRole);

        log.info("권한 자동 위임 완료 - delegatedRole: {}, newManagerMemberId: {}",
                delegatedRole, newManager.getId());
    }

    /**
     * 팀원 목록 조회
     */
    @Transactional(readOnly = true)
    public TeamMemberListResponse getTeamMembers(Long teamRoomId, Long userId) {

        // 1. 팀룸 조회
        teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 해당 팀룸의 멤버인지 확인
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 팀원 목록 조회 (User 정보 포함 - 기존 Fetch Join 활용)
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamRoomId(teamRoomId);

        // 4. DTO 변환 (LEADER = HOST > MEMBER, 동일 순위 내 입장일 기준)
        List<TeamMemberListResponse.MemberSummary> memberSummaries = teamMembers.stream()
                .sorted(Comparator.comparingInt((TeamMember m) -> m.getTeamRole() == TeamRole.MEMBER ? 1 : 0)
                        .thenComparing(TeamMember::getCreatedAt))
                .map(member -> new TeamMemberListResponse.MemberSummary(
                        member.getId(),
                        member.getUser().getId(),
                        member.getUser().getName(),
                        member.getUser().getMajor(),
                        member.getUser().getProfileImageId(),
                        member.getTeamRole()
                ))
                .toList();

        return new TeamMemberListResponse(memberSummaries);
    }

    /**
     * 팀원 상세 조회
     */
    @Transactional(readOnly = true)
    public TeamMemberDetailResponse getTeamMemberDetail(Long teamRoomId, Long userId, Long memberId) {

        // 1. 팀룸 조회
        teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 해당 팀룸의 멤버인지 확인
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 팀룸 ID + 멤버 ID로 직접 조회
        TeamMember targetMember = teamMemberRepository.findByIdAndTeamRoomId(memberId, teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 4. DTO 변환
        User user = targetMember.getUser();
        return new TeamMemberDetailResponse(
                targetMember.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMajor(),
                user.getMbti(),
                targetMember.getCreatedAt()
        );
    }
}