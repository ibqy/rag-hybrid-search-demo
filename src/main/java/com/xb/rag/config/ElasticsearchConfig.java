package com.xb.rag.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 客户端配置
 *
 * 手动创建 RestClient → Transport → ElasticsearchClient 链路，
 * 不使用 Spring Data Elasticsearch 的自动配置，避免版本冲突。
 *
 * @author ibqy
 */
@Configuration
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        HttpHost host = HttpHost.create(uris);
        Header[] headers = new Header[]{new BasicHeader("Content-Type", "application/json")};

        RestClient restClient = RestClient.builder(host)
                .setDefaultHeaders(headers)
                .setRequestConfigCallback(cb -> cb
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000))
                .build();

        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        ElasticsearchClient client = new ElasticsearchClient(transport);
        log.info("ES 客户端已创建: uris={}", uris);
        return client;
    }
}