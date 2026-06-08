package com.doccase.tag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.doccase.tag", "com.doccase.common"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan({"com.doccase.tag.mapper", "com.doccase.tag.rule.mapper"})
public class TagApplication {

    public static void main(String[] args) {
        SpringApplication.run(TagApplication.class, args);
    }
}
