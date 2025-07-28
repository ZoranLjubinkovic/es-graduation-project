package com.griddynamics.product.indexer.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductRepositoryImpl implements ProductRepository {

    private RestHighLevelClient esClient;

    @Value("${com.griddynamics.product.indexer.index}")
    private String indexName;
    @Value("${com.griddynamics.product.indexer.alias}")
    private String aliasName;
    @Value("${com.griddynamics.product.indexer.maxIndices:3}")
    private Integer maxIndices;

    // Mappings, settings and bulk data files
    @Value("${com.griddynamics.product.indexer.files.mappings:classpath:elastic/products/mappings.json}")
    private Resource productsMappingsFile;
    @Value("${com.griddynamics.product.indexer.files.settings:classpath:elastic/products/settings.json}")
    private Resource productsSettingsFile;
    @Value("${com.griddynamics.product.indexer.files.bulkData:classpath:elastic/products/bulk_data.txt}")
    private Resource productsBulkInsertDataFile;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ProductRepositoryImpl(RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public void recreateIndex() {
        String indexNameWithUnderscoreAndDateFormatted =
                indexName + "_" +
                        LocalDateTime.now()
                                .format(formatter);
        log.info("New name for index, indexNameWithUnderscoreAndDateFormatted: {}",
                indexNameWithUnderscoreAndDateFormatted
        );

        String settings = getStrFromResource(productsSettingsFile);
        String mappings = getStrFromResource(productsMappingsFile);
        createIndex(indexNameWithUnderscoreAndDateFormatted, settings, mappings);
        fixIndicesAndAliases(indexNameWithUnderscoreAndDateFormatted);

        processBulkInsertData(productsBulkInsertDataFile);
    }

    private boolean fixIndicesAndAliases(String indexNameWithUnderscoreAndDateFormatted) {
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest("*");
            GetIndexResponse response  = esClient.indices().get(getIndexRequest, RequestOptions.DEFAULT);

            List<String> indices = Arrays.asList(response.getIndices());

            String indexNamePrefixPlusUnderscore = indexName + "_";

            List<String> filteredIndices =
                    indices
                            .stream()
                            .filter(index -> {
                                if (!index.startsWith(indexNamePrefixPlusUnderscore)) {
                                    return false;
                                }
                                try {
                                    LocalDateTime.parse(
                                            index.substring(indexNamePrefixPlusUnderscore.length()),
                                            ProductRepository.formatter
                                    );
                                    return true;
                                } catch (DateTimeParseException e) {
                                    return false;
                                }
                            })
                            .sorted(Collections.reverseOrder())
                            .toList();

            List<String> indicesToRemove =
                    filteredIndices
                            .stream()
                            .skip(5)
                            .toList();

            IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();

            IndicesAliasesRequest.AliasActions aliasActionAdd =
                    IndicesAliasesRequest.AliasActions
                            .add()
                            .index(indexNameWithUnderscoreAndDateFormatted)
                            .alias(indexName);
            indicesAliasesRequest.addAliasAction(aliasActionAdd);

            for (String indexToRemove : indicesToRemove) {
                IndicesAliasesRequest.AliasActions aliasActionRemoveIndex =
                        IndicesAliasesRequest.AliasActions
                                .removeIndex()
                                .index(indexToRemove);
                indicesAliasesRequest.addAliasAction(aliasActionRemoveIndex);
            }

            if (filteredIndices.size() > 1) {
                String indexNameForAliasRemoval = filteredIndices.get(1);
                IndicesAliasesRequest.AliasActions removeAlias =
                        IndicesAliasesRequest.AliasActions
                                .remove()
                                .alias(indexName)
                                .index(indexNameForAliasRemoval);
                indicesAliasesRequest.addAliasAction(removeAlias);

            }
            AcknowledgedResponse acknowledgedResponse =
                    esClient
                            .indices()
                            .updateAliases(indicesAliasesRequest, RequestOptions.DEFAULT);

            return acknowledgedResponse.isAcknowledged();

        } catch (Exception e) {
            log.error("fixIndicesAndAliases error ", e);
            return false;
        }
    }

    private static String getStrFromResource(Resource resource) {
        try {
            if (!resource.exists()) {
                throw new IllegalArgumentException("File not found: " + resource.getFilename());
            }
            return Resources.toString(resource.getURL(), Charsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Can not read resource file: " + resource.getFilename(), ex);
        }

    }

    private void createIndex(String indexName, String settings, String mappings) {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName)
                .mapping(mappings, XContentType.JSON)
                .settings(settings, XContentType.JSON);

        CreateIndexResponse createIndexResponse;
        try {
            createIndexResponse = esClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new RuntimeException("An error occurred during creating ES index.", ex);
        }

        if (!createIndexResponse.isAcknowledged()) {
            throw new RuntimeException("Creating index not acknowledged for indexName: " + indexName);
        } else {
            log.info("Index {} has been created.", indexName);
        }
    }


    private void processBulkInsertData(Resource bulkInsertDataFile) {
        int requestCnt = 0;
        try {
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

            String bulkInsertDataJsonContent = StreamUtils.copyToString(bulkInsertDataFile.getInputStream(), StandardCharsets.UTF_8);

            List<IndexRequest> indexRequestsFromJsonArray = createIndexRequests(bulkInsertDataJsonContent);
            for(IndexRequest indexRequest : indexRequestsFromJsonArray) {
                bulkRequest.add(indexRequest);
                requestCnt++;
            }

            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.getItems().length != requestCnt) {
                log.warn("Only {} out of {} requests have been processed in a bulk request.", bulkResponse.getItems().length, requestCnt);
            } else {
                log.info("{} requests have been processed in a bulk request.", bulkResponse.getItems().length);
            }

            if (bulkResponse.hasFailures()) {
                log.warn("Bulk data processing has failures:\n{}", bulkResponse.buildFailureMessage());
            }
        } catch (IOException ex) {
            log.error("An exception occurred during bulk data processing", ex);
            throw new RuntimeException(ex);
        }
    }

    private List<IndexRequest> createIndexRequests(String jsonContent) {
        List<IndexRequest> requests = new ArrayList<>();

        try {
            JsonNode arrayNode = objectMapper.readTree(jsonContent);

            if (!arrayNode.isArray()) {
                log.warn("Expected JSON array but got: {}", arrayNode.getNodeType());
                return requests;
            }

            for (JsonNode productNode : arrayNode) {
                IndexRequest request = createIndexRequestFromProduct(productNode);
                if (request != null) {
                    requests.add(request);
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to parse JSON array: {}", ex.getMessage());
        }

        return requests;
    }

    private IndexRequest createIndexRequestFromProduct(JsonNode productNode) {
        try {
            String productId = productNode.get("id").asText();
            String productJson = objectMapper.writeValueAsString(productNode);
            return new IndexRequest()
                    .index(aliasName)
                    .id(productId)
                    .source(productJson, XContentType.JSON);

        } catch (Exception ex) {
            log.warn("Failed to create IndexRequest for product: {}", ex.getMessage());
            return null;
        }
    }

}
