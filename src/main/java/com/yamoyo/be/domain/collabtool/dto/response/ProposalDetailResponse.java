package com.yamoyo.be.domain.collabtool.dto.response;

import com.yamoyo.be.domain.collabtool.entity.ToolProposal;
import com.yamoyo.be.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "협업툴 제안 상세 조회 응답")
public record ProposalDetailResponse(

        @Schema(description = "제안 ID", example = "10")
        Long proposalId,

        @Schema(description = "협업툴 카테고리 ID", example = "1")
        Integer categoryId,

        @Schema(description = "협업툴 ID", example = "5")
        Integer toolId,

        @Schema(description = "제안자 사용자 ID", example = "3")
        Long requestedBy,

        @Schema(description = "제안자 이름", example = "홍길동")
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