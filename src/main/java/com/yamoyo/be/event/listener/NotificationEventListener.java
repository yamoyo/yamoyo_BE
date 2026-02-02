package com.yamoyo.be.event.listener;

import com.yamoyo.be.domain.fcm.entity.UserDevice;
import com.yamoyo.be.domain.fcm.repository.UserDeviceRepository;
import com.yamoyo.be.domain.fcm.service.FcmService;
import com.yamoyo.be.domain.notification.entity.Notification;
import com.yamoyo.be.domain.notification.entity.NotificationType;
import com.yamoyo.be.domain.notification.service.NotificationService;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.event.event.NotificationEvent;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final FcmService fcmService;
    private final UserDeviceRepository userDeviceRepository;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("알림 이벤트 수신: type={}, teamRoomId={}", event.type(), event.teamRoomId());
        TeamRoom teamRoom = teamRoomRepository.findById(event.teamRoomId())
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        List<User> receivers = teamMemberRepository.findByTeamRoomId(event.teamRoomId())
                .stream()
                .map(TeamMember::getUser)
                .collect(Collectors.toList());

        // 팀룸 입장 알림의 경우 본인은 제외
        if(NotificationType.TEAM_JOIN.equals(event.type()) && event.targetId() != null) {
            receivers.removeIf(user -> user.getId().equals(event.targetId()));
        }

        List<Long> receiverIds = receivers.stream()
                .map(User::getId)
                .toList();

        if (receivers.isEmpty()) return;

        // 알림 제목
        String title = event.type().generateTitle(teamRoom.getTitle());

        // 알림 메시지
        String message = event.type().generateDescription(teamRoom.getTitle());

        // 수신자의 모든 토큰 리스트
        List<String> allTokens = new ArrayList<>();

        // 수신자 객체 및 기기 한 번에 조회
        Map<Long, List<UserDevice>> deviceMap = userDeviceRepository.findAllByUserIdIn(receiverIds)
                .stream()
                .collect(Collectors.groupingBy(device -> device.getUser().getId()));

        // 알림 엔티티 리스트 생성
        List<Notification> notifications = new ArrayList<>();

        for (User user : receivers) {
            // 알림 엔티티 생성
            notifications.add(Notification.create(
                    user,
                    teamRoom,
                    event.targetId(),
                    event.type(),
                    title,
                    message
            ));

            // FCM 토큰 수집 (알람 설정이 켜진 사용자만)
            if (user.isAlarmOn()) {
                deviceMap.getOrDefault(user.getId(), Collections.emptyList())
                        .forEach(d -> allTokens.add(d.getFcmToken()));
            }
        }

        // 알림 일괄 저장
        try {
            notificationService.saveAll(notifications);
        } catch (Exception e) {
            log.error("알림 일괄 저장 중 에러 발생: {}", e.getMessage());
        }

        // FCM 발송
        if (!allTokens.isEmpty()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", event.type().name());
            data.put("teamRoomId", String.valueOf(event.teamRoomId()));
            if (event.targetId() != null) {
                data.put("targetId", String.valueOf(event.targetId()));
            }

            // 500개씩 끊어서 보내는 로직은 FCM SDK 내부 혹은 Service 에서 처리
            fcmService.sendMessage(allTokens, title, message, data);
        }
    }
}
