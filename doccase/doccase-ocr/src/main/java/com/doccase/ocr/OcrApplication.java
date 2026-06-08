package com.doccase.ocr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"com.doccase.ocr", "com.doccase.common"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@MapperScan("com.doccase.ocr.mapper")
public class OcrApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcrApplication.class, args);
    }
}
