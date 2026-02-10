package com.novaTech.Nova.Services.Configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "getstream")
@Data
public class GetStreamConfig {

    private String apiKey = "adnr3brnnw24";
    private String apiSecret = "5bzckae8jzctrfr5f76r9ztaa9kt3tnw5sanmvnbafm6j8nsbxy7h84qhzkupuk3";
}