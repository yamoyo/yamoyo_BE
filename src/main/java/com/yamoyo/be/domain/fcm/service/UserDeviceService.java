package com.yamoyo.be.domain.fcm.service;

import com.yamoyo.be.domain.fcm.entity.UserDevice;
import com.yamoyo.be.domain.fcm.repository.UserDeviceRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void updateDeviceStatus(Long userId, String fcmToken, String deviceType, String deviceName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        // 이미 있다면 마지막 로그인 시간만 업데이트
        userDeviceRepository.findByFcmToken(fcmToken)
                        .ifPresentOrElse(
                                device -> {
                                    device.changeUser(user);
                                    device.updateDeviceDetails(deviceType, deviceName);
                                    device.updateLastLogin();
                                },
                                // 존재하지 않을 경우 새로운 기기 등록
                                () -> {
                                    UserDevice newDevice = UserDevice.create(
                                       user,
                                       fcmToken,
                                       deviceType,
                                       deviceName
                                    );
                                    userDeviceRepository.save(newDevice);
                                }
                        );
    }
}
