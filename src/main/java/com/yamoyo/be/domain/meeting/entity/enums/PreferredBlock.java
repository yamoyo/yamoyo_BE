package com.yamoyo.be.domain.meeting.entity.enums;

import lombok.Getter;

@Getter
public enum PreferredBlock {
    BLOCK_08_12(0, 7),
    BLOCK_12_16(8, 15),
    BLOCK_16_20(16, 23),
    BLOCK_20_24(24, 31);

    private final int startSlotIndex;
    private final int endSlotIndex;

    PreferredBlock(int startSlotIndex, int endSlotIndex) {
        this.startSlotIndex = startSlotIndex;
        this.endSlotIndex = endSlotIndex;
    }

    public boolean containsOneHourMeetingAt(int slotIndex) {
        int meetingEndSlotIndex = slotIndex + 1;
        return slotIndex >= startSlotIndex && meetingEndSlotIndex <= endSlotIndex;
    }
}
