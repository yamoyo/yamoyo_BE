package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.request.JoinTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.*;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import com.yamoyo.be.domain.teamroom.repository.BannedTeamMemberRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamRoomService {

    private final UserRepository userRepository;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BannedTeamMemberRepository bannedTeamMemberRepository;
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

    /**
     * 초대 링크 생성 로직
     * - 재발급 시 사용
     */
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

        log.info("사용자 userId : {} 팀룸 입장 시작", userId);

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
        if (bannedTeamMemberRepository.existsByTeamRoomIdAndUserId(teamRoomId, userId)) {
            throw new YamoyoException(ErrorCode.BANNED_MEMBER);
        }

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

    /**
     * 팀룸 목록 조회 (진행중/완료 구분)
     * 
     * @param userId 사용자 ID
     * @param lifecycle 라이프사이클 (ACTIVE: 진행중, ARCHIVED: 완료)
     * @return 팀룸 목록
     */
    public List<TeamRoomListResponse> getTeamRoomList(Long userId, Lifecycle lifecycle) {
        log.info("팀룸 목록 조회 시작 - userId: {}, lifecycle: {}", userId, lifecycle);

        // 1. 사용자가 속한 팀룸 조회 (이미 최신순 정렬됨)
        List<TeamRoom> teamRooms = teamMemberRepository.findTeamRoomsByUserIdAndLifecycle(userId, lifecycle);

        // 2. 각 팀룸에 대한 상세 정보 조회 및 DTO 변환
        return teamRooms.stream()
                .map(teamRoom -> {
                    // 전체 팀원 조회
                    List<TeamMember> members = teamMemberRepository.findByTeamRoomId(teamRoom.getId());

                    // 팀원 요약 정보 생성(userId, 프로필 이미지)
                    List<TeamRoomListResponse.MemberSummary> memberSummaries = members.stream()
                            .map(member -> new TeamRoomListResponse.MemberSummary(
                                    member.getUser().getId(),
                                    member.getUser().getProfileImageId()
                            )).collect(Collectors.toList());

                    // 응답 DTO 생성
                    return new TeamRoomListResponse(
                            teamRoom.getId(),
                            teamRoom.getTitle(),
                            teamRoom.getBannerImageId(),
                            teamRoom.getCreatedAt(),
                            teamRoom.getDeadline(),
                            teamRoom.getLifecycle(),
                            members.size(),
                            memberSummaries
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 팀룸 상세 조회
     *
     * @param teamRoomId 조회할 팀룸 ID
     * @param userId 요청자 ID (권한 체크용)
     * @return 팀룸 상세 정보
     */
    public TeamRoomDetailResponse getTeamRoomDetail(Long teamRoomId, Long userId) {
        log.info("팀룸 상세 조회 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 팀원인지 확인 (권한 체크)
        TeamMember myMember = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 전체 팀원 조회
        List<TeamMember> members = teamMemberRepository.findByTeamRoomId(teamRoomId);

        // 4. 팀원 요약 정보 생성(HOST → LEADER → MEMBER 순 정렬)
        List<TeamRoomDetailResponse.MemberSummary> memberSummaries = members.stream()
                .map(member -> new TeamRoomDetailResponse.MemberSummary(
                        member.getUser().getId(),
                        member.getUser().getName(),
                        member.getUser().getProfileImageId(),
                        member.getTeamRole()
                ))
                .sorted(Comparator.comparing(TeamRoomDetailResponse.MemberSummary::role))
                .collect(Collectors.toList());

        // 5. 응답 DTO 생성
        return new TeamRoomDetailResponse(
                teamRoom.getId(),
                teamRoom.getTitle(),
                teamRoom.getDescription(),
                teamRoom.getDeadline(),
                teamRoom.getBannerImageId(),
                teamRoom.getCreatedAt(),
                teamRoom.getLifecycle(),
                teamRoom.getWorkflow(),
                members.size(),
                memberSummaries,
                myMember.getTeamRole()
        );
    }

    /**
     * 팀룸 수정
     */
    @Transactional
    public void updateTeamRoom(Long teamRoomId, CreateTeamRoomRequest request, Long userId) {
        log.info("팀룸 수정 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 권한 체크
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!member.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 3. 마감일 검증
        if (request.deadline().isBefore(LocalDateTime.now().plusDays(1))) {
            throw new YamoyoException(ErrorCode.INVALID_DEADLINE);
        }

        // 4. 전체 수정
        teamRoom.update(request.title(), request.description(), request.deadline(), request.bannerImageId());

        log.info("팀룸 수정 완료 - teamRoomId: {}", teamRoomId);
    }

    /**
     * 팀룸 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteTeamRoom(Long teamRoomId, Long userId) {
        log.info("팀룸 삭제 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 권한 체크 (HOST 또는 LEADER만 가능)
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!member.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 3. 소프트 삭제 (lifecycle → DELETED)
        teamRoom.delete();

        log.info("팀룸 삭제 완료 - teamRoomId: {}", teamRoomId);
    }
}
