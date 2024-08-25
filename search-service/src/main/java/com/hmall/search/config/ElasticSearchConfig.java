package com.hmall.search.config;

import lombok.Data;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.IOException;

@Data
@Configuration
@ConfigurationProperties(prefix = "hm.elasticsearch")
public class ElasticSearchConfig {

    private String url;

    private RestHighLevelClient client;

    @Bean
    public RestHighLevelClient client() {
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create(url)
        ));
    }

    @PreDestroy
    public void close() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
    }
}
