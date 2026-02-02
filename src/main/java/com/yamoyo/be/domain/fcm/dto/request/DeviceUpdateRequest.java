package com.yamoyo.be.domain.fcm.dto.request;

public record DeviceUpdateRequest(
       String fcmToken,     // 필수
       String deviceType,   // "ANDROID" | "IOS" | "WEB"
       String deviceName    // "iPhone 15 Pro" 등
) {
}
