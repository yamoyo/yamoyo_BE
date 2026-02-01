package com.yamoyo.be.domain.meeting.everytime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class EverytimeConfig {

    @Bean
    public RestClient everytimeRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.everytime.kr")
                .defaultHeader("Origin", "https://everytime.kr")
                .defaultHeader("Referer", "https://everytime.kr/")
                .defaultHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(Duration.ofSeconds(3));
                    setReadTimeout(Duration.ofSeconds(3));
                }})
                .build();
    }
}
