package com.yamoyo.be.domain.teamroom.entity;

import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "team_members",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_team_room_user",
            columnNames = {"team_room_id", "user_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false)
    private TeamRole teamRole;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private TeamMember(TeamRoom teamRoom, User user, TeamRole teamRole) {
        this.teamRoom = teamRoom;
        this.user = user;
        this.teamRole = teamRole;
    }

    /**
     * 팀룸 생성자 → HOST 권한 부여
     */
    public static TeamMember createHost(TeamRoom teamRoom, User user){
        return new TeamMember(teamRoom, user, TeamRole.HOST);
    }

    /**
     * 초대링크로 입장 → MEMBER 권한 부여
     */
    public static TeamMember createMember(TeamRoom teamRoom, User user){
        return new TeamMember(teamRoom, user, TeamRole.MEMBER);
    }

    // ===== 비즈니스 로직 =====
    /**
     * 역할 변경
     */
    public void changeTeamRole(TeamRole newRole){
        if(newRole == null){
            throw new IllegalStateException("역할은 필수입니다.");
        }
        this.teamRole = newRole;
    }

    /**
     * 관리 권한 확인 (방장 or 팀장)
     */
    public boolean hasManagementAuthority(){
        return this.teamRole == TeamRole.HOST || this.teamRole == TeamRole.LEADER;
    }

    /**
     * HOST → MEMBER 강등 (팀장 선출 완료 후)
     */
    public void demoteToMember() {
        if (this.teamRole != TeamRole.HOST) {
            throw new IllegalStateException("HOST만 MEMBER로 강등할 수 있습니다");
        }
        this.teamRole = TeamRole.MEMBER;
    }

    /**
     * MEMBER → LEADER 승격 (팀장 당첨)
     */
    public void promoteToLeader() {
        this.teamRole = TeamRole.LEADER;
    }

}
