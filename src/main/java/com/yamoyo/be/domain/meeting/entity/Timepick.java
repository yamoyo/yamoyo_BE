package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.TimepickStatus;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "timepicks")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Timepick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timepick_id", updatable = false, nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false, unique = true)
    private TeamRoom teamRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimepickStatus status;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Timepick(TeamRoom teamRoom, TimepickStatus status, LocalDateTime deadline) {
        this.teamRoom = teamRoom;
        this.status = status;
        this.deadline = deadline;
    }

    public static Timepick create(TeamRoom teamRoom, LocalDateTime deadline) {
        return Timepick.builder()
                .teamRoom(teamRoom)
                .status(TimepickStatus.OPEN)
                .deadline(deadline)
                .build();
    }

    public boolean isFinalized() {
        return this.status == TimepickStatus.FINALIZED;
    }

    public void markAsFinalized() {
        this.status = TimepickStatus.FINALIZED;
        this.finalizedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
