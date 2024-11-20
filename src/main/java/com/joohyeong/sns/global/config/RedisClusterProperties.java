package com.joohyeong.sns.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;


@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis.cluster")
public class RedisClusterProperties {

    private String password;
    private int maxRedirects;
    private List<String> nodes;
}