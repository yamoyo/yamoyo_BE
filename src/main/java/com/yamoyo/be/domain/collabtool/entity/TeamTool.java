package com.yamoyo.be.domain.collabtool.entity;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_tools")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamTool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id")
    private TeamRoom teamRoom;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "tool_id")
    private Integer toolId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
