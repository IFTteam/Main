package springredis.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableScheduling
public class ProjectConfiguration {
    @Value("${SPARKPOST_API_KEY}")
    private String SPARKPOST_API_KEY;

    @Value("${SP_BASE_URI}")
    private String SP_BASE_URI;

    @Bean
    public WebClient webClient(){
        return WebClient.builder()
                .baseUrl(SP_BASE_URI)
                .defaultHeader("Authorization", SPARKPOST_API_KEY)
                .build();
    }
}
