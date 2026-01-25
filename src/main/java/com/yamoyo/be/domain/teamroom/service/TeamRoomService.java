package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.CreateTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
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
}
