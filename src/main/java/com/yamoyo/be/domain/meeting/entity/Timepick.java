package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.TimepickStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
