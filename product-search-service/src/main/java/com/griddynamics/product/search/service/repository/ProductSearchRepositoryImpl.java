package com.griddynamics.product.search.service.repository;

import com.griddynamics.product.search.service.rest.ProductSearchRequest;
import com.griddynamics.product.search.service.rest.ProductSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    @Value("${com.griddynamics.product.search.service.alias}")
    private String alias;

    private RestHighLevelClient esClient;

    public ProductSearchRepositoryImpl(RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public ProductSearchResponse search(ProductSearchRequest request) {
        QueryBuilder queryBuilder = processQueryText(request.getQueryText());
        return executeSearch(queryBuilder, request);
    }

    private QueryBuilder processQueryText(String queryText) {
        List<QueryBuilder> queryBuilders = new ArrayList<>();
        if (queryText != null && !queryText.trim().isEmpty()) {

            String[] tokens = queryText.toLowerCase().split("\\s+");

            List<String> colorsFound = new LinkedList<>();
            List<String> sizesFound = new LinkedList<>();
            Map<Integer, String> tokensFound = new HashMap<>();

            int cnt = 0;
            for (String token : tokens) {
                if (sizes.contains(token)) {
                    sizesFound.add(token.toUpperCase());
                } else if (colors.contains(token)) {
                    colorsFound.add(StringUtils.capitalize(token));
                } else {
                    tokensFound.put(cnt, token);
                }
                cnt++;
            }

            List<QueryBuilder> sizeAndColorQueryBuilders = createSizesAndColorsQuery(sizesFound, colorsFound);
            sizeAndColorQueryBuilders.forEach(queryBuilders::add);

            List<String> phrases = processTokens(tokensFound);
            QueryBuilder regularTokensQuery = createTokensQuery(tokensFound.values(), phrases);
            queryBuilders.add(regularTokensQuery);

            BoolQueryBuilder result = QueryBuilders.boolQuery();
            queryBuilders.forEach(result::must);
            return result;
        } else {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
        }
    }

    public List<String> processTokens(Map<Integer, String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> phrases = new ArrayList<>();
        List<Integer> positions = new ArrayList<>(tokens.keySet());

        List<String> currentPhraseTokens = new ArrayList<>();
        int lastPosition = -1;

        for (int position : positions) {
            if (lastPosition == -1 || position == lastPosition + 1) {
                currentPhraseTokens.add(tokens.get(position));
            } else {
                if (!currentPhraseTokens.isEmpty()) {
                    phrases.add(String.join(" ", currentPhraseTokens));
                    currentPhraseTokens.clear();
                }
                currentPhraseTokens.add(tokens.get(position));
            }
            lastPosition = position;
        }

        if (currentPhraseTokens.size() > 1) {
            phrases.add(String.join(" ", currentPhraseTokens));
        }

        return phrases;
    }

    private static QueryBuilder createTokensQuery(Collection<String> tokens, List<String> phrases) {
        String allTokens = String.join(" ", tokens);
        BoolQueryBuilder regularTokensQuery = QueryBuilders.boolQuery();
        QueryBuilder tokensQuery = QueryBuilders.multiMatchQuery(allTokens)
                .field(NAME_FIELD)
                .field(BRAND_FIELD)
                .field(BRAND_SHINGLES)
                .field(NAME_SHINGLES)
                .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                .operator(Operator.AND);
        regularTokensQuery.must(tokensQuery);

        phrases.forEach(phrase -> {
            QueryBuilder phraseQuery = QueryBuilders.multiMatchQuery(phrase)
                    .field(BRAND_SHINGLES, 5.0f)
                    .field(NAME_SHINGLES, 5.0f)
                    .type(MultiMatchQueryBuilder.Type.PHRASE);
            regularTokensQuery.should(phraseQuery);
        });

        return regularTokensQuery;
    }

    private static List<QueryBuilder> createSizesAndColorsQuery(List<String> foundSizes, List<String> foundColors) {

        List<QueryBuilder> queryBuilders = new ArrayList<>();

        if (!foundSizes.isEmpty() && !foundColors.isEmpty()) {
            BoolQueryBuilder allSizeColorsCombinationsQuery = QueryBuilders.boolQuery();
            for (String sizeToken : foundSizes) {
                for (String colorToken : foundColors) {
                    NestedQueryBuilder sizeColorCombinationQuery = QueryBuilders.nestedQuery("skus",
                            QueryBuilders.boolQuery()
                                    .must(QueryBuilders.termQuery(SKUS_COLOR_FIELD, colorToken).boost(3.0f))
                                    .must(QueryBuilders.termQuery(SKUS_SIZE_FIELD, sizeToken).boost(2.0f)),
                            ScoreMode.Max);
                    allSizeColorsCombinationsQuery.should(sizeColorCombinationQuery);
                    allSizeColorsCombinationsQuery.minimumShouldMatch(1);
                    queryBuilders.add(allSizeColorsCombinationsQuery);
                }
            }

            queryBuilders.add(allSizeColorsCombinationsQuery);

        } else if (!foundSizes.isEmpty()) {
            foundSizes.forEach(token -> {
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("skus",
                        QueryBuilders.termQuery(SKUS_SIZE_FIELD, token)
                                .boost(2.0f), ScoreMode.Max);
                queryBuilders.add(nestedQuery);
            });

        } else if (!foundColors.isEmpty()) {
            foundColors.forEach(token -> {
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("skus",
                        QueryBuilders.termQuery(SKUS_COLOR_FIELD, token)
                                .boost(3.0f), ScoreMode.Max);
                queryBuilders.add(nestedQuery);
            });

        }
        return queryBuilders;
    }

    private ProductSearchResponse executeSearch(QueryBuilder mainQuery, ProductSearchRequest request) {
        SearchSourceBuilder ssb =
                new SearchSourceBuilder()
                        .query(mainQuery)
                        .size(request.getSize())
                        .from(request.getPage() * request.getSize());

        ssb.sort(new FieldSortBuilder(SCORE_FIELD).order(SortOrder.DESC));
        ssb.sort(new FieldSortBuilder(ID_FIELD).order(SortOrder.DESC));

        List<AggregationBuilder> aggs = createAggregations();
        aggs.forEach(ssb::aggregation);

        SearchRequest searchRequest = new SearchRequest(alias).source(ssb);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
            return toProductSearchResponse(searchResponse);
        } catch (Exception e) {
            log.error("GET PRODUCTS ERROR", e);
            return ProductSearchResponse.builder().build();
        }
    }

    private ProductSearchResponse toProductSearchResponse(SearchResponse searchResponse) {

        Long totalHits = searchResponse.getHits().getTotalHits().value;

        List<Map<String, Object>> products =
                Arrays.stream(searchResponse.getHits().getHits())
                        .map(SearchHit::getSourceAsMap)
                        .toList();

        List<Map<String, Object>> facets = extractFacets(searchResponse);

        return ProductSearchResponse.builder()
                .totalHits(totalHits)
                .products(products)
                .facets(facets)
                .build();
    }

    private List<Map<String, Object>> extractFacets(SearchResponse searchResponse) {
        List<Map<String, Object>> aggregations = new ArrayList<>();

        // brand
        List<Map<String, Object>> brandCountAggregation = extractTermAggregation(searchResponse, BRAND_COUNT_AGG);
        aggregations.add(Map.of(BRAND_FIELD, brandCountAggregation));

        // price
        List<Map<String, Object>> priceRangeAggregation = extractRangeAggregation(searchResponse, PRICE_RANGE_AGG);
        aggregations.add(Map.of(PRICE_FIELD, priceRangeAggregation));

        // skus.color
        List<Map<String, Object>> colorAggregation = extractNestedAggregation(searchResponse, SKUS_COLOR_NESTED_AGG);
        aggregations.add(Map.of("color", colorAggregation));

        // skus.size
        List<Map<String, Object>> sizeAggregation = extractNestedAggregation(searchResponse, SKUS_SIZE_NESTED_AGG);
        aggregations.add(Map.of("size", sizeAggregation));

        return aggregations;
    }

    private List<Map<String, Object>> extractTermAggregation(SearchResponse searchResponse, String aggregationName) {
        List<Map<String, Object>> aggregations = new ArrayList<>();
        ParsedTerms parsedTerms = searchResponse.getAggregations().get(aggregationName);

        parsedTerms.getBuckets().stream()
                .sorted(Comparator.comparingLong(Terms.Bucket::getDocCount)
                        .reversed()
                        .thenComparing(Terms.Bucket::getKeyAsString))
                .forEach(bucket -> {
                    Map<String, Object> brandCounts = new LinkedHashMap<>();
                    brandCounts.put("value", bucket.getKeyAsString());
                    brandCounts.put("count", bucket.getDocCount());
                    aggregations.add(brandCounts);
                });

        return aggregations;
    }

    private List<Map<String, Object>> extractRangeAggregation(SearchResponse searchResponse, String aggregationName) {
        ParsedRange parsedRange = searchResponse.getAggregations().get(aggregationName);
        List<Map<String, Object>> ranges = new ArrayList<>();
        parsedRange.getBuckets()
                .forEach(bucket -> {
                    Map<String, Object> rangeValues = new HashMap<>();
                    rangeValues.put("value", bucket.getKeyAsString());
                    rangeValues.put("to", bucket.getTo());
                    rangeValues.put("from", bucket.getFrom());
                    rangeValues.put("count", bucket.getDocCount());
                    ranges.add(rangeValues);
                });

        return ranges;
    }

    private List<Map<String, Object>> extractNestedAggregation(SearchResponse searchResponse, String aggregationName) {
        List<Map<String, Object>> aggregations = new ArrayList<>();
        ParsedNested parsedNested = searchResponse.getAggregations().get(aggregationName);

        Terms terms = parsedNested.getAggregations().get(aggregationName);
        terms.getBuckets().forEach(bucket -> {
            ReverseNested reverseNested = bucket.getAggregations().get(PRODUCT_COUNT);
            long productCount = reverseNested.getDocCount();

            Map<String, Object> colorBucket = new LinkedHashMap<>();
            colorBucket.put("value", bucket.getKeyAsString());
            colorBucket.put("count", productCount);
            aggregations.add(colorBucket);
        });

        return aggregations.stream().sorted(
                Comparator.<Map<String, Object>>comparingLong(m -> (Long) m.get("count")).reversed()
                        .thenComparing(m -> (String) m.get("value"))
        ).collect(Collectors.toList());
    }

    private List<AggregationBuilder> createAggregations() {
        List<AggregationBuilder> aggregationsList = new ArrayList<>();

        // brand aggregation
        TermsAggregationBuilder brandCountAgg = AggregationBuilders
                .terms(BRAND_COUNT_AGG)
                .field(BRAND_FIELD);
        aggregationsList.add(brandCountAgg);

        // price range
        RangeAggregationBuilder priceRangeAgg = AggregationBuilders
                .range(PRICE_RANGE_AGG)
                .field(PRICE_FIELD)
                .keyed(true)
                .addRange(new RangeAggregator.Range("Cheap", 0.0, 100.0))
                .addRange(new RangeAggregator.Range("Average", 100.0, 500.0))
                .addRange(new RangeAggregator.Range("Expensive", 500.0, null));
        aggregationsList.add(priceRangeAgg);

        // skus color nested
        NestedAggregationBuilder skusColorAgg = AggregationBuilders
                .nested(SKUS_COLOR_NESTED_AGG, "skus")
                .subAggregation(
                        AggregationBuilders.terms(SKUS_COLOR_NESTED_AGG)
                                .field(SKUS_COLOR_FIELD)
                                .subAggregation(AggregationBuilders.reverseNested(PRODUCT_COUNT))
                );
        aggregationsList.add(skusColorAgg);

        // skus size nested
        NestedAggregationBuilder skusSizeAgg = AggregationBuilders
                .nested(SKUS_SIZE_NESTED_AGG, "skus")
                .subAggregation(
                        AggregationBuilders.terms(SKUS_SIZE_NESTED_AGG)
                                .field(SKUS_SIZE_FIELD)
                                .subAggregation(AggregationBuilders.reverseNested(PRODUCT_COUNT))
                );
        aggregationsList.add(skusSizeAgg);

        return aggregationsList;
    }
}
