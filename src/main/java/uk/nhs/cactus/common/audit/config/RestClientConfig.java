package uk.nhs.cactus.common.audit.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    @Bean
    public RestTemplate auditRestTemplate() {
        // Currently only used for local-only services (i.e. audit server)
        // a timeout of 50 should be acceptable locally
        var timeout = Duration.ofMillis(50);
        return new RestTemplateBuilder()
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

}
