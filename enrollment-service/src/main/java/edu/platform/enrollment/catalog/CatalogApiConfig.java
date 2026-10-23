package edu.platform.enrollment.catalog;

import edu.platform.enrollment.CatalogProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class CatalogApiConfig {

    @Bean
    public CatalogApi catalogApi(CatalogProperties properties, WebClient.Builder builder) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 250)
                .responseTimeout(Duration.ofMillis(250));

        WebClient webClient = builder
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();
        return factory.createClient(CatalogApi.class);
    }
}