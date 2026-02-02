package com.yamoyo.be.domain.fcm.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    // 공식 문서 기준 한 번에 보낼 수 있는 최대 토큰 수
    private static final int MAX_TOKEN_BATCH_SIZE = 500;

    public void sendMessage(List<String> tokens, String title, String body, Map<String, String> data) {
        if(tokens.isEmpty()) return;

        // 500개씩 리스트를 분할하여 처리
        for(int i = 0; i < tokens.size(); i += MAX_TOKEN_BATCH_SIZE) {
            int end = Math.min(i + MAX_TOKEN_BATCH_SIZE, tokens.size());
            List<String> subTokens = tokens.subList(i, end);

            sendBatch(subTokens, title, body, data);
        }
    }

    private void sendBatch(List<String> tokens, String title, String body, Map<String, String> data) {
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 발송 결과 : 성공 {}건, 실패 {}건", response.getSuccessCount(), response.getFailureCount());

            if(response.getFailureCount() > 0) {
                handleFailures(tokens, response);
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패", e);
        }
    }

    private void handleFailures(List<String> tokens, BatchResponse response) {
        for(int i = 0; i < response.getResponses().size(); i++) {
            if(!response.getResponses().get(i).isSuccessful()) {
                String failedToken = tokens.get(i);
                log.warn("발송 실패 토큰: {}, 사유: {}",
                        failedToken, response.getResponses().get(i).getException().getMessage());
            }
        }
    }
}
