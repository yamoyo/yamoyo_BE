package com.yamoyo.be.domain.collabtool.dto.response;

import java.util.List;

/**
 * 확정된 협업툴 조회 응답
 */
public record ConfirmedToolsResponse(
        List<CategoryTools> confirmedTools
) {
    public record CategoryTools(
            Integer categoryId,
            List<Integer> confirmedToolIds
    ) {}
}