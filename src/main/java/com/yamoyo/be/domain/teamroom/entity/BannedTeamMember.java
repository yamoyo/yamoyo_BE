package com.yamoyo.be.domain.teamroom.entity;

import com.yamoyo.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "banned_team_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"team_room_id", "user_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannedTeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ban_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    @PrePersist
    protected void onCreate() {
        this.bannedAt = LocalDateTime.now();
    }

    // 정적 팩토리 메서드
    public static BannedTeamMember create(TeamRoom teamRoom, User user) {
        BannedTeamMember banned = new BannedTeamMember();
        banned.teamRoom = teamRoom;
        banned.user = user;
        return banned;
    }
}
