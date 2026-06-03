// 文件说明：商铺搜索服务，使用 Elasticsearch 维护商铺搜索索引。

package com.hmdp.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopSearchService {

    private static final String SHOP_INDEX = "hmdp_shop";

    private final RestHighLevelClient client;

    public void ensureIndex() throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest(SHOP_INDEX);
        if (client.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return;
        }

        CreateIndexRequest createIndexRequest = new CreateIndexRequest(SHOP_INDEX);
        createIndexRequest.source("{\n" +
                "  \"settings\": {\n" +
                "    \"number_of_shards\": 1,\n" +
                "    \"number_of_replicas\": 0\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "    \"properties\": {\n" +
                "      \"id\": {\"type\": \"long\"},\n" +
                "      \"name\": {\"type\": \"text\", \"fields\": {\"keyword\": {\"type\": \"keyword\"}}},\n" +
                "      \"typeId\": {\"type\": \"long\"},\n" +
                "      \"area\": {\"type\": \"text\", \"fields\": {\"keyword\": {\"type\": \"keyword\"}}},\n" +
                "      \"address\": {\"type\": \"text\"},\n" +
                "      \"sold\": {\"type\": \"integer\"},\n" +
                "      \"score\": {\"type\": \"integer\"},\n" +
                "      \"avgPrice\": {\"type\": \"long\"}\n" +
                "    }\n" +
                "  }\n" +
                "}", XContentType.JSON);
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        log.info("创建商铺 ES 索引完成，index={}", SHOP_INDEX);
    }

    public void indexShops(List<Shop> shops) throws IOException {
        if (CollUtil.isEmpty(shops)) {
            return;
        }
        ensureIndex();
        BulkRequest bulkRequest = new BulkRequest();
        for (Shop shop : shops) {
            if (shop == null || shop.getId() == null) {
                continue;
            }
            bulkRequest.add(buildIndexRequest(shop));
        }
        if (bulkRequest.numberOfActions() == 0) {
            return;
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("批量写入商铺 ES 索引完成，数量={}", bulkRequest.numberOfActions());
    }

    public void indexShop(Shop shop) throws IOException {
        if (shop == null || shop.getId() == null) {
            return;
        }
        ensureIndex();
        client.index(buildIndexRequest(shop), RequestOptions.DEFAULT);
    }

    public List<Long> searchShopIdsByName(String name, Integer current, Integer size) throws IOException {
        ensureIndex();

        int page = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((page - 1) * pageSize)
                .size(pageSize);

        if (StrUtil.isBlank(name)) {
            sourceBuilder.query(QueryBuilders.matchAllQuery())
                    .sort("sold", SortOrder.DESC)
                    .sort("score", SortOrder.DESC);
        } else {
            sourceBuilder.query(QueryBuilders.multiMatchQuery(name, "name", "area", "address"));
        }

        SearchRequest searchRequest = new SearchRequest(SHOP_INDEX).source(sourceBuilder);
        SearchHit[] hits = client.search(searchRequest, RequestOptions.DEFAULT).getHits().getHits();
        List<Long> ids = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            ids.add(Long.valueOf(hit.getId()));
        }
        return ids;
    }

    private IndexRequest buildIndexRequest(Shop shop) {
        return new IndexRequest(SHOP_INDEX)
                .id(shop.getId().toString())
                .source(JSONUtil.toJsonStr(toDocument(shop)), XContentType.JSON);
    }

    private Map<String, Object> toDocument(Shop shop) {
        Map<String, Object> document = new HashMap<>();
        document.put("id", shop.getId());
        document.put("name", shop.getName());
        document.put("typeId", shop.getTypeId());
        document.put("area", shop.getArea());
        document.put("address", shop.getAddress());
        document.put("sold", shop.getSold());
        document.put("score", shop.getScore());
        document.put("avgPrice", shop.getAvgPrice());
        return document;
    }
}
