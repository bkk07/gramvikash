package com.learn.lld.gramvikash.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a RestTemplate bean used by DiagnosticService and IVRSService
 * to call the Python FastAPI service.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10 s
        factory.setReadTimeout(60_000);      // 60 s  (ML inference can be slow)
        return new RestTemplate(factory);
    }
}
