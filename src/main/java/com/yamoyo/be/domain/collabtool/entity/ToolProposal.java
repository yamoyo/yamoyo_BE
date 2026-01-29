package com.yamoyo.be.domain.collabtool.entity;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_tool_proposals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ToolProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoom;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;  // 제안자 userId

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "tool_id", nullable = false)
    private Integer toolId;

    /**
     * 승인 상태
     * tinyint(1): 0(PENDING), 1(APPROVED), 2(REJECTED)
     */
    @Column(name = "is_approval", nullable = false, columnDefinition = "TINYINT")
    private Integer isApproval;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;  // 승인/반려 결정 시각

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 협업툴 제안 생성
     */
    public static ToolProposal create(TeamRoom teamRoom, Integer categoryId, Integer toolId, Long requestedBy) {
        ToolProposal proposal = new ToolProposal();
        proposal.teamRoom = teamRoom;
        proposal.categoryId = categoryId;
        proposal.toolId = toolId;
        proposal.requestedBy = requestedBy;
        proposal.isApproval = 0;  // PENDING
        return proposal;
    }

    /**
     * 승인/반려 처리
     * @param isApproved true: 승인, false: 반려
     */
    public void updateApprovalStatus(boolean isApproved) {
        this.isApproval = isApproved ? 1 : 2;
        this.decidedAt = LocalDateTime.now();
    }

    /**
     * 승인 대기 중인지 확인
     */
    public boolean isPending() {
        return this.isApproval == 0;
    }
}