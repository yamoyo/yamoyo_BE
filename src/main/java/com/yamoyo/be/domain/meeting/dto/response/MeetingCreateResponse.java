package com.yamoyo.be.domain.meeting.dto.response;

import java.util.List;

public record MeetingCreateResponse(
        Long meetingSeriesId,
        List<Long> meetingIds,
        Integer createdMeetingCount
) {}
