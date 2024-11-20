package com.joohyeong.sns.global.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "db.datasource")
public class DBProperty {
    @NotEmpty
    private String writeurl;
    @NotEmpty
    private List<String> readurls;
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;
    @NotEmpty
    private String driver;
}