package com.yamoyo.be.domain.collabtool.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_tool_votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberToolVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_vote_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "team_room_id", nullable = false)
    private Long teamRoomId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "tool_id", nullable = false)
    private Integer toolId;

    public static MemberToolVote create(Long memberId, Long teamRoomId, Integer categoryId, Integer toolId) {
        MemberToolVote vote = new MemberToolVote();
        vote.memberId = memberId;
        vote.teamRoomId = teamRoomId;
        vote.categoryId = categoryId;
        vote.toolId = toolId;
        return vote;
    }
}