package org.openoa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {
    // 配置 RestTemplate Bean，用于发送 HTTP 请求（调用钉钉 API）
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}