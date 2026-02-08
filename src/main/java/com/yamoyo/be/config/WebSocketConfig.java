package com.yamoyo.be.config;

import com.yamoyo.be.common.interceptor.StompHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 prefix ("/user"는 convertAndSendToUser용)
        // TaskScheduler 설정: 스케줄러 스레드에서 보낸 메시지도 정상 처리되도록 함
        // Heartbeat 설정: 서버 10초, 클라이언트 10초 간격으로 heartbeat 전송
        config.enableSimpleBroker("/sub", "/queue")
                .setTaskScheduler(brokerTaskScheduler())
                .setHeartbeatValue(new long[]{10000, 10000});
        // 클라이언트가 메시지를 보낼 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/pub");
        // convertAndSendToUser에서 사용하는 prefix (기본값: /user)
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Simple Broker용 TaskScheduler
     * - Heartbeat 처리
     * - 스케줄러 스레드에서 보낸 메시지 처리
     */
    @Bean
    public TaskScheduler brokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-broker-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * WebSocket 연결 엔드포인트 설정
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns(
                        "https://yamoyo.kr", "*"
                )
                .withSockJS();
    }

    /**
     * 클라이언트로부터 들어오는 메시지 처리 설정
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

    /**
     * 클라이언트로 나가는 메시지 처리 설정 (디버깅용)
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String destination = accessor.getDestination();
                String sessionId = accessor.getSessionId();
                log.info("[Outbound] destination={}, sessionId={}, command={}",
                        destination, sessionId, accessor.getCommand());
                return message;
            }
        });
    }
}