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

            try (InputStream inputStream = firebaseKeyPath.startsWith("classpath:")
                    ? new ClassPathResource(firebaseKeyPath.replace("classpath:", "")).getInputStream()
                    : new FileInputStream(firebaseKeyPath)) {

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp 초기화 성공");

            } catch (IOException e) {
                log.warn("Firebase 키 파일을 찾을 수 없습니다. 경로: {}, 에러: {}", firebaseKeyPath, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Firebase 초기화 중 예상치 못한 오류 발생", e);
        }
    }
}

