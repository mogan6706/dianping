// 文件说明：Elasticsearch 客户端配置。

package com.hmdp.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient(
            @Value("${hmdp.elasticsearch.uris:http://localhost:9200}") String uris
    ) {
        String[] uriArray = uris.split(",");
        HttpHost[] hosts = new HttpHost[uriArray.length];
        for (int i = 0; i < uriArray.length; i++) {
            hosts[i] = HttpHost.create(uriArray[i].trim());
        }
        return new RestHighLevelClient(RestClient.builder(hosts));
    }
}
