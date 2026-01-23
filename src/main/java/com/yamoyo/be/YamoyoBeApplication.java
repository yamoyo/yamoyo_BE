package com.yamoyo.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class YamoyoBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(YamoyoBeApplication.class, args);
    }

}
