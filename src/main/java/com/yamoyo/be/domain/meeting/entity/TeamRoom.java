package com.yamoyo.be.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: domain.team에 TeamRoom 엔티티 생성 시 이 stub 삭제
@Table(name = "team_rooms")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_room_id", updatable = false, nullable = false)
    private Long id;
}
