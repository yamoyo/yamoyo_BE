package com.yamoyo.be.domain.meeting.dto.response;

import com.yamoyo.be.domain.meeting.entity.enums.UpdateScope;

import java.util.List;

public record MeetingUpdateResponse(
        UpdateScope scope,
        Integer updatedMeetingCount,
        List<Long> updatedMeetingIds,
        List<Long> skippedMeetingIds
) {}
