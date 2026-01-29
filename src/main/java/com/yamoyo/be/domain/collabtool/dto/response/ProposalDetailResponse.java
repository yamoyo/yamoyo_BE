package com.yamoyo.be.domain.collabtool.dto.response;

import com.yamoyo.be.domain.collabtool.entity.ToolProposal;
import com.yamoyo.be.domain.user.entity.User;

/**
 * 제안 상세 조회 응답 (단일)
 */
public record ProposalDetailResponse(
        Long proposalId,
        Integer categoryId,
        Integer toolId,
        Long requestedBy,
        String proposerName
) {
    public static ProposalDetailResponse from(ToolProposal proposal, User proposer) {
        return new ProposalDetailResponse(
                proposal.getId(),
                proposal.getCategoryId(),
                proposal.getToolId(),
                proposal.getRequestedBy(),
                proposer.getName()
        );
    }
}