package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember,Long> {


    /**
     * 사용자가 속한 팀룸 조회 (라이프사이클 필터링)
     * - 홈 화면: 참여 중 팀룸 목록 : ACTIVE
     * - 마이페이지: 완료된 팀룸 목록 : ARCHIVED
     */
    @Query("""
            SELECT tm.teamRoom
            FROM TeamMember tm
            WHERE tm.user.id = :userId
              AND tm.teamRoom.lifecycle = :lifecycle
            ORDER BY tm.teamRoom.createdAt DESC
           """)
    List<TeamRoom> findTeamRoomsByUserIdAndLifecycle(@Param("userId") Long userId,
                                                    @Param("lifecycle") Lifecycle lifecycle);

    /**
     * 팀룸 내 특정 사용자 조회
     * - 해당 사용자가 팀원인지 확인
     * - 해당 팀원의 역할 확인
     */
    Optional<TeamMember> findByTeamRoomIdAndUserId(Long teamRoomId, Long userId);

    /**
     * 팀룸 멤버 수 카운트 (12명 정원 체크)
     */
    long countByTeamRoomId(Long teamRoomId);

    /**
     * 팀룸 전체 멤버 조회
     */
    List<TeamMember> findByTeamRoomId(Long teamRoomId);

    /**
     * 팀룸 전체 멤버 조회 - 정렬 추가
     */
    List<TeamMember> findByTeamRoomId(Long teamRoomId, Sort sort);

    /**
     * 특정 역할 조회 (팀장/방장 찾기)
     */
    Optional<TeamMember> findByTeamRoomIdAndTeamRole(Long teamRoomId, TeamRole teamRole);

}
