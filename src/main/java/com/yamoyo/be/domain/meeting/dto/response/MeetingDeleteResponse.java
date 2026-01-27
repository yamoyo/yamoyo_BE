package com.yamoyo.be.domain.meeting.dto.response;

import com.yamoyo.be.domain.meeting.entity.enums.UpdateScope;

import java.util.List;

public record MeetingDeleteResponse(
        UpdateScope scope,
        Integer deletedMeetingCount,
        List<Long> deletedMeetingIds,
        Boolean seriesDeleted
) {}
