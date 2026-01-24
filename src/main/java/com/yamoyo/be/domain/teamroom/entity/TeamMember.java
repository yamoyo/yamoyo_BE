package com.yamoyo.be.domain.teamroom.entity;

import com.yamoyo.be.domain.teamroom.enums.TeamRole;
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
    private User userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false)
    private TeamRole teamRole;

    @Column(name = "created_at")
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

    /**
     * 팀룸 생성자 → HOST 권한 부여
     */
    public static TeamMember createHost(TeamRoom teamRoom, User user){
        TeamMember teamMember = new TeamMember();
        teamMember.teamRoomId = teamRoom;
        teamMember.userId = user;
        teamMember.teamRole = TeamRole.HOST;
        return teamMember;
    }

    /**
     * 초대링크로 입장 → MEMBER 권한 부여
     */
    public static TeamMember createMember(TeamRoom teamRoom, User user){
        TeamMember teamMember = new TeamMember();
        teamMember.teamRoomId = teamRoom;
        teamMember.userId = user;
        teamMember.teamRole = TeamRole.MEMBER;
        return teamMember;
    }
}
