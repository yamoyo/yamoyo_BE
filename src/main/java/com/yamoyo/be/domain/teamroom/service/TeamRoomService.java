package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.request.JoinTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.CreateTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.dto.response.InviteLinkResponse;
import com.yamoyo.be.domain.teamroom.dto.response.JoinTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.enums.Workflow;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamRoomService {

    private final UserRepository userRepository;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final InviteTokenService inviteTokenService;

    private static final long TOKEN_EXPIRATION_SECONDS = 86400L; // 24시간

    /**
     * 팀룸 생성 로직
     *
     * 마감일 검증 후 팀룸 생성 처리 시작
     * - 유저 정보와 팀룸 입력값으로 팀룸 객체 생성
     * - 생성자를 방장으로 등록 (HOST)
     * - 초대 링크(토큰만) 생성 후 반환
     */
    @Transactional
    public CreateTeamRoomResponse createTeamRoom(CreateTeamRoomRequest request, Long userId) {

        log.info("팀룸 생성 시작 사용자 userId: {}", userId);

        // 마감일 검증 (현재 시간 +1일 이후)
        if (request.deadline().isBefore(LocalDateTime.now().plusDays(1))) {
            throw new YamoyoException(ErrorCode.INVALID_DEADLINE);
        }

        // 1. user 정보
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        // 2. 팀룸 생성
        TeamRoom teamRoom = TeamRoom.builder()
                .title(request.title())
                .description(request.description())
                .deadline(request.deadline())
                .bannerImageId(request.bannerImageId())
                .build();
        teamRoomRepository.save(teamRoom);

        // 3. 생성자를 방장으로 등록
        TeamMember host = TeamMember.createHost(teamRoom, user);
        teamMemberRepository.save(host);

        // 4. 초대링크 생성
        String inviteToken = inviteTokenService.createInviteToken(teamRoom.getId());

        log.info("팀룸이 성공적으로 생성되었습니다. teamRoomId: {}", teamRoom.getId());

        return new CreateTeamRoomResponse(
                teamRoom.getId(),
                inviteToken,
                TOKEN_EXPIRATION_SECONDS
        );
    }

    @Transactional
    public InviteLinkResponse issueInviteLink(Long teamRoomId, Long userId) {

        // 1. 팀룸 존재 + ACTIVE 체크
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        if (teamRoom.getLifecycle() != Lifecycle.ACTIVE) {
            throw new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND);
        }

        // 2. 요청자가 팀원인지 확인
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        log.info("초대 링크 발급 요청 - teamRoomId={}, userId={}", teamRoomId, userId);

        // 3. 토큰 재발급(기존 무효화 포함) - 이미 구현되어 있다고 했으니 그대로 호출
        String token = inviteTokenService.createInviteToken(teamRoomId);

        return new InviteLinkResponse(token, TOKEN_EXPIRATION_SECONDS);
    }


    /**
     * 팀룸 입장 로직
     * - 팀룸 입장 예외처리 상세는 문서 참고
     */
    @Transactional
    public JoinTeamRoomResponse joinTeamRoom(JoinTeamRoomRequest request, Long userId) {

        log.info("사용자 userId : {} 팀룸 입장", userId);

        // 1. 토큰에서 팀룸 정보 검증
        String token = request.inviteToken();
        Long teamRoomId = inviteTokenService.getTeamRoomIdByToken(token);
        if (teamRoomId == null) {
            throw new YamoyoException(ErrorCode.INVITE_INVALID);
        }

        // 2. 팀룸 조회 (ACTIVE 상태만)
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));
        if(teamRoom.getLifecycle() != Lifecycle.ACTIVE){
            throw new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND);
        }

        log.info("팀룸 정보 확인. teamRoomId: {}, title : {}", teamRoomId, teamRoom.getTitle());

        // 3. 팀룸 작업 상황에 따른 입장 처리
        if(teamRoom.getWorkflow() == Workflow.LEADER_SELECTION
                || teamRoom.getWorkflow() == Workflow.SETUP){
            throw new YamoyoException(ErrorCode.TEAMROOM_JOIN_FORBIDDEN);
        }

        // 4. 이미 멤버일 경우
        Optional<TeamMember> exist = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId);
        if (exist.isPresent()) {
            return new JoinTeamRoomResponse(
                    JoinTeamRoomResponse.JoinResult.ENTERED,
                    teamRoomId,
                    exist.get().getId()
            );
        }

        // 5. 정원 체크
        long count = teamMemberRepository.countByTeamRoomId(teamRoomId);
        if(count >= 12) throw new YamoyoException(ErrorCode.TEAMROOM_FULL);

        //  6. 밴 여부 확인
        // ===== code =====

        // user 정보
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        // 7. 신규 팀원 생성 - MEMBER 추가
        TeamMember newTeamMember = TeamMember.createMember(teamRoom, user);
        teamMemberRepository.save(newTeamMember);

        log.info("팀룸에 참가하였습니다. memberId: {}", newTeamMember.getId());

        return new JoinTeamRoomResponse(
                JoinTeamRoomResponse.JoinResult.JOINED,
                teamRoomId,
                newTeamMember.getId()
        );
    }
}
