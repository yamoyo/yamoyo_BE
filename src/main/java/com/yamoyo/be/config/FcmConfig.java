package com.yamoyo.be.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${firebase.key-path}")
    private String firebaseKeyPath;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("FirebaseApp 이미 초기화됨");
                return;
            }

            InputStream inputStream;
            // 클래스패스(classpath:)로 시작하는 경우
            if (firebaseKeyPath.startsWith("classpath:")) {
                String path = firebaseKeyPath.replace("classpath:", "");
                inputStream = new ClassPathResource(path).getInputStream();
            } else {
                // 그 외에는 파일 시스템 경로로 처리
                try {
                    inputStream = new FileInputStream(firebaseKeyPath);
                } catch (Exception e) {
                    // 로컬 개발 환경 등 파일이 없을 경우를 대비해 예외 처리하되,
                    // 실제 운영 환경에서는 파일이 필수이므로 에러 로그를 명확히 남김
                    log.warn("Firebase 키 파일을 찾을 수 없습니다. 경로: {}, 에러: {}", firebaseKeyPath, e.getMessage());
                    return;
                }
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp 초기화 성공");

        } catch (IOException e) {
            log.error("Firebase 초기화 실패", e);
            // 필수 기능이라면 여기서 예외를 던져서 앱 구동을 막을 수도 있음
            // throw new RuntimeException("Firebase init failed", e);
        }
    }
}
